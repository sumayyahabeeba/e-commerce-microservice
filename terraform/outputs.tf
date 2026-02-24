output "kubernetes_cluster_name" {
  description = "GKE Cluster Name"
  value       = google_container_cluster.primary.name
}

output "kubernetes_cluster_host" {
  description = "GKE Cluster API endpoint"
  value       = "https://${google_container_cluster.primary.endpoint}"
  sensitive   = true
}

output "artifact_registry_url" {
  description = "Artifact Registry URL for Docker images"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/ecommerce-repo"
}

output "project_id" {
  description = "GCP Project ID"
  value       = var.project_id
}

output "region" {
  description = "GCP Region"
  value       = var.region
}

output "zone" {
  description = "GCP Zone"
  value       = var.zone
}

output "cicd_service_account_email" {
  description = "Email of the CI/CD service account"
  value       = google_service_account.cicd_sa.email
}

output "gke_connect_command" {
  description = "Command to connect kubectl to this cluster"
  value       = "gcloud container clusters get-credentials ${var.cluster_name} --zone ${var.zone} --project ${var.project_id}"
}
