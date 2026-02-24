terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.0"
    }
  }

  # Remote state in GCS — create this bucket manually first:
  # gsutil mb -p YOUR_PROJECT_ID -l us-central1 gs://tf-state-ecommerce
  backend "gcs" {
    bucket = "tf-state-ecommerce"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project = var.project_id
  region  = var.region
}

# ─────────────────────────────────────────────────────────────────
# VPC NETWORK
# ─────────────────────────────────────────────────────────────────
resource "google_compute_network" "vpc" {
  name                    = "${var.cluster_name}-vpc"
  auto_create_subnetworks = false
  description             = "VPC for ${var.cluster_name} GKE cluster"
}

resource "google_compute_subnetwork" "subnet" {
  name          = "${var.cluster_name}-subnet"
  ip_cidr_range = "10.10.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc.name
  description   = "Subnet for GKE nodes"

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.20.0.0/16"
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.30.0.0/20"
  }

  private_ip_google_access = true
}

# ─────────────────────────────────────────────────────────────────
# FIREWALL RULES
# ─────────────────────────────────────────────────────────────────
resource "google_compute_firewall" "allow_internal" {
  name    = "${var.cluster_name}-allow-internal"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }
  allow {
    protocol = "udp"
    ports    = ["0-65535"]
  }
  allow {
    protocol = "icmp"
  }

  source_ranges = ["10.10.0.0/24", "10.20.0.0/16", "10.30.0.0/20"]
}

# ─────────────────────────────────────────────────────────────────
# GKE CLUSTER
# ─────────────────────────────────────────────────────────────────
resource "google_container_cluster" "primary" {
  name     = var.cluster_name
  location = var.zone

  # We manage the node pool separately
  remove_default_node_pool = true
  initial_node_count       = 1

  network    = google_compute_network.vpc.name
  subnetwork = google_compute_subnetwork.subnet.name

  ip_allocation_policy {
    cluster_secondary_range_name  = "pods"
    services_secondary_range_name = "services"
  }

  # Workload Identity for secure access to GCP services
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  # Addons
  addons_config {
    http_load_balancing {
      disabled = false
    }
    horizontal_pod_autoscaling {
      disabled = false
    }
    gce_persistent_disk_csi_driver_config {
      enabled = true
    }
  }

  # Release channel for managed upgrades
  release_channel {
    channel = "REGULAR"
  }

  # Maintenance window
  maintenance_policy {
    daily_maintenance_window {
      start_time = "03:00"
    }
  }

  # Logging and monitoring
  logging_service    = "logging.googleapis.com/kubernetes"
  monitoring_service = "monitoring.googleapis.com/kubernetes"

  # Disable basic auth
  master_auth {
    client_certificate_config {
      issue_client_certificate = false
    }
  }

  # Network policy
  network_policy {
    enabled  = false
    provider = "PROVIDER_UNSPECIFIED"
  }

  lifecycle {
    ignore_changes = [initial_node_count]
  }
}

# ─────────────────────────────────────────────────────────────────
# NODE POOL
# ─────────────────────────────────────────────────────────────────
resource "google_container_node_pool" "primary_nodes" {
  name       = "${var.cluster_name}-node-pool"
  location   = var.zone
  cluster    = google_container_cluster.primary.name
  node_count = var.node_count

  autoscaling {
    min_node_count = 1
    max_node_count = 5
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  upgrade_settings {
    max_surge       = 1
    max_unavailable = 0
  }

  node_config {
    machine_type = var.machine_type
    disk_size_gb = 50
    disk_type    = "pd-standard"
    image_type   = "COS_CONTAINERD"

    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/trace.append",
      "https://www.googleapis.com/auth/compute",
    ]

    labels = {
      env     = var.environment
      project = var.cluster_name
    }

    metadata = {
      disable-legacy-endpoints = "true"
    }

    shielded_instance_config {
      enable_secure_boot = true
    }
  }
}

# ─────────────────────────────────────────────────────────────────
# ARTIFACT REGISTRY
# ─────────────────────────────────────────────────────────────────
resource "google_artifact_registry_repository" "ecommerce_repo" {
  location      = var.region
  repository_id = "ecommerce-repo"
  description   = "Docker repository for ecommerce microservices"
  format        = "DOCKER"

  labels = {
    environment = var.environment
  }
}

# ─────────────────────────────────────────────────────────────────
# SERVICE ACCOUNT FOR CI/CD
# ─────────────────────────────────────────────────────────────────
resource "google_service_account" "cicd_sa" {
  account_id   = "cicd-sa"
  display_name = "CI/CD Service Account"
  description  = "Service account for GitHub Actions and Harness CI/CD"
}

resource "google_project_iam_member" "cicd_artifact_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.cicd_sa.email}"
}

resource "google_project_iam_member" "cicd_gke_developer" {
  project = var.project_id
  role    = "roles/container.developer"
  member  = "serviceAccount:${google_service_account.cicd_sa.email}"
}

resource "google_project_iam_member" "cicd_storage_admin" {
  project = var.project_id
  role    = "roles/storage.objectAdmin"
  member  = "serviceAccount:${google_service_account.cicd_sa.email}"
}
