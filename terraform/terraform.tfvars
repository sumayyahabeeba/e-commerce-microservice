# ─────────────────────────────────────────────────────────────────
# terraform.tfvars — Replace values with your actual GCP config
# NOTE: Do NOT commit this file with real values.
#       Add terraform.tfvars to .gitignore in production.
# ─────────────────────────────────────────────────────────────────

project_id   = "brilliant-tide-453616-r9"
region       = "us-central1"
zone         = "us-central1-a"
cluster_name = "ecommerce-cluster"
node_count   = 2
machine_type = "e2-medium"
environment  = "dev"
