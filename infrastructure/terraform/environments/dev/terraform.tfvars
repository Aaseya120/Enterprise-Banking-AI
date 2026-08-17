# environments/dev/terraform.tfvars
# Development environment — smaller instances, single-AZ RDS, 1 MSK broker.
# Set sensitive values via environment variables (TF_VAR_*) or CI/CD secrets.

environment = "dev"
aws_region  = "us-east-1"

# Smaller instances to reduce dev cost
eks_system_node_type     = "t3.medium"
eks_service_node_type    = "t3.xlarge"
eks_service_node_min     = 2
eks_service_node_max     = 6
eks_service_node_desired = 3

rds_instance_class    = "db.t3.micro"
rds_multi_az          = false
rds_deletion_protection = false

msk_instance_type = "kafka.t3.small"
msk_broker_count  = 1   # single broker (no HA) in dev

domain_name     = "dev.banking.example.com"
route53_zone_id = ""    # leave blank to skip DNS/cert provisioning in dev

# These must be set via environment variables or CI secrets — do NOT hardcode here:
# TF_VAR_rds_master_password
# TF_VAR_jwt_signing_key
# TF_VAR_encryption_passphrase
# TF_VAR_fraud_decision_hmac_secret
