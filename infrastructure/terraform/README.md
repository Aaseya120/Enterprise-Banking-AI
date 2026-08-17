# ☁️ Terraform AWS Infrastructure

Welcome to the Infrastructure-as-Code (IaC) guide! This directory contains the blueprint for deploying the Enterprise Banking AI Platform to AWS using **Terraform**.

## 🏗️ Architecture Overview

The platform is deployed into a secure, highly-available Virtual Private Cloud (VPC):

```text
VPC (10.0.0.0/16)
  ├── 🌐 Public Subnets (3 AZs): Application Load Balancer (ALB), NAT Gateway
  └── 🔒 Private Subnets (3 AZs):
        ├── EKS Cluster (Kubernetes for all microservices)
        ├── 13 × RDS PostgreSQL 16 (Dedicated database per service)
        ├── MSK Kafka Cluster (Event streaming)
        └── VPC Endpoints (S3, ECR, Secrets Manager)
```

---

## 🛠️ Prerequisites

Make sure you have Terraform and the AWS CLI installed:

```bash
brew install terraform awscli
aws configure  # Authenticate with your AWS account
```

### One-Time Setup: Remote State
Before running Terraform for the first time, you must create the S3 bucket and DynamoDB table used to store and lock the Terraform state securely:

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

---

## 🚀 Deploying an Environment

### Development Environment

Navigate to the terraform directory:
```bash
cd infrastructure/terraform
terraform init
```

Generate a plan (replacing `<secret>` with your actual secure passwords):
```bash
terraform plan \
  -var-file="environments/dev/terraform.tfvars" \
  -var="rds_master_password=<secret>" \
  -var="jwt_signing_key=<secret>" \
  -var="encryption_passphrase=<secret>" \
  -var="fraud_decision_hmac_secret=<secret>" \
  -out=tfplan
```

Apply the plan:
```bash
terraform apply tfplan
```

### Production Environment
The process is identical, but you will point to the production variables file:
```bash
terraform plan -var-file="environments/prod/terraform.tfvars" ...
```

---

## 🔑 Useful Outputs

Once `terraform apply` finishes, it will print helpful outputs. You can retrieve them later with these commands:

```bash
# Update your local kubeconfig to access the new EKS cluster
aws eks update-kubeconfig --region us-east-1 --name $(terraform output -raw eks_cluster_name)

# Get the Kafka MSK bootstrap brokers (needed for Helm values)
terraform output -raw msk_bootstrap_brokers

# List all created ECR Repository URLs (for pushing Docker images)
terraform output -json ecr_repository_urls
```

---

## 🛡️ Security Best Practices Enforced

- **No Passwords in State Files**: Sensitive values are marked as `sensitive = true` in Terraform.
- **IAM Roles for Service Accounts (IRSA)**: Pods access AWS Secrets Manager securely via IAM roles, meaning no long-lived AWS keys are ever stored on nodes or inside containers.
- **Strict SSL**: `rds.force_ssl=1` ensures that all microservices must connect to PostgreSQL using SSL.
- **KMS Envelope Encryption**: Kubernetes secrets are double-encrypted at rest using AWS KMS.

> **⚠️ Note:** This Terraform configuration is a reference blueprint. While it follows AWS best practices, you should always review and run `terraform plan` to understand exactly what resources will be created in your specific AWS account before applying.
