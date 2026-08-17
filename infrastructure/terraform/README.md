# Terraform AWS Infrastructure

## Architecture

```
VPC (10.0.0.0/16)
  ├── Public subnets (3 AZs): ALB, NAT gateway
  └── Private subnets (3 AZs):
        ├── EKS cluster (system + services node groups)
        ├── 13 × RDS PostgreSQL 16 (one per stateful service)
        ├── MSK Kafka cluster (1 broker dev / 3 broker prod)
        └── VPC Endpoints: S3, ECR, Secrets Manager
```

## Prerequisites

```bash
brew install terraform awscli
aws configure  # set up credentials for your AWS account
```

Create the Terraform state backend resources (one-time, manual):
```bash
aws s3 mb s3://banking-platform-tfstate --region us-east-1
aws s3api put-bucket-versioning --bucket banking-platform-tfstate \
  --versioning-configuration Status=Enabled
aws dynamodb create-table --table-name banking-platform-tflock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## Deploy dev environment

```bash
cd infrastructure/terraform

terraform init

terraform plan \
  -var-file="environments/dev/terraform.tfvars" \
  -var="rds_master_password=<secret>" \
  -var="jwt_signing_key=<secret>" \
  -var="encryption_passphrase=<secret>" \
  -var="fraud_decision_hmac_secret=<secret>" \
  -out=tfplan

terraform apply tfplan
```

## Deploy prod environment

Same as dev but with the prod tfvars file and stronger secrets from your
secrets management solution:

```bash
terraform plan \
  -var-file="environments/prod/terraform.tfvars" \
  ...
```

## Key outputs

After `terraform apply`, retrieve key values:
```bash
# EKS kubeconfig
aws eks update-kubeconfig \
  --region us-east-1 \
  --name $(terraform output -raw eks_cluster_name)

# MSK bootstrap brokers (set in Helm values)
terraform output -raw msk_bootstrap_brokers

# ECR URLs (used in CI/CD to push images)
terraform output -json ecr_repository_urls
```

## Security notes

- **No secrets in state**: sensitive values are marked `sensitive = true` —
  they appear in state as `(sensitive value)` but are still stored in the
  state file (which is encrypted server-side in the S3 backend). Avoid
  printing state files to logs.
- **IRSA**: services access Secrets Manager via IAM Roles for Service Accounts —
  no long-lived credentials on nodes or in pods.
- **RDS SSL**: `rds.force_ssl=1` in the parameter group; all JDBC connections
  must use `ssl=require` in the connection string (services' `application.yml`
  should add `?ssl=require` to the datasource URL in prod profile).
- **EKS envelope encryption**: Kubernetes Secrets are envelope-encrypted with KMS.
- **ECR image scanning**: scan-on-push enabled; review findings in ECR console.

## Scope caveat

This Terraform has not been `apply`'d against a live AWS account — it is
carefully-written HCL that follows AWS provider conventions and has been
reviewed for correctness, but should be treated as a blueprint to validate
(`terraform validate`, `terraform plan`) before running in your account.
