# environments/prod/terraform.tfvars
# Production environment — Multi-AZ RDS, 3-broker MSK, larger EKS nodes,
# deletion protection enabled, DNS/ACM provisioned.

environment = "prod"
aws_region  = "us-east-1"

# Larger instances for production load
eks_system_node_type     = "t3.large"
eks_service_node_type    = "m6i.xlarge"
eks_service_node_min     = 3
eks_service_node_max     = 20
eks_service_node_desired = 6

rds_instance_class      = "db.r6g.large"
rds_multi_az            = true   # standby in second AZ for HA
rds_deletion_protection = true   # prevent accidental drops

msk_instance_type = "kafka.m5.large"
msk_broker_count  = 3   # one broker per AZ

domain_name     = "banking.example.com"
route53_zone_id = "ZXXXXXXXXXXXXX"   # replace with your real hosted zone ID

# Sensitive values injected at deploy time by CI/CD (GitHub Secrets -> TF_VAR_*):
# TF_VAR_rds_master_password
# TF_VAR_jwt_signing_key
# TF_VAR_encryption_passphrase
# TF_VAR_fraud_decision_hmac_secret
