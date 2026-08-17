# ─── Environment ──────────────────────────────────────────────────────────────

variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod"
  }
}

variable "project" {
  description = "Project name prefix for all resource names"
  type        = string
  default     = "banking"
}

# ─── Networking ───────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of AZs to use for subnets"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (ALB, NAT gateway)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (EKS nodes, RDS, MSK)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24", "10.0.13.0/24"]
}

# ─── EKS ──────────────────────────────────────────────────────────────────────

variable "eks_cluster_version" {
  description = "Kubernetes version for EKS"
  type        = string
  default     = "1.30"
}

variable "eks_system_node_type" {
  description = "Instance type for system node group (CoreDNS, kube-proxy, etc.)"
  type        = string
  default     = "t3.medium"
}

variable "eks_service_node_type" {
  description = "Instance type for services node group (banking microservices)"
  type        = string
  default     = "t3.xlarge"
}

variable "eks_service_node_min" {
  description = "Minimum nodes in the services node group"
  type        = number
  default     = 2
}

variable "eks_service_node_max" {
  description = "Maximum nodes in the services node group"
  type        = number
  default     = 10
}

variable "eks_service_node_desired" {
  description = "Desired nodes in the services node group"
  type        = number
  default     = 3
}

# ─── RDS ──────────────────────────────────────────────────────────────────────

variable "rds_instance_class" {
  description = "RDS instance class for all per-service databases"
  type        = string
  default     = "db.t3.micro"  # dev; override to db.t3.medium or db.r6g.large in prod
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ for RDS (false in dev to reduce cost)"
  type        = bool
  default     = false
}

variable "rds_deletion_protection" {
  description = "Enable deletion protection on RDS instances"
  type        = bool
  default     = false  # set to true in prod
}

variable "rds_master_password" {
  description = "Master password for all RDS instances"
  type        = string
  sensitive   = true
  # Set via: terraform apply -var="rds_master_password=<value>"
  # Or via TF_VAR_rds_master_password env var.
  # In prod: use AWS Secrets Manager rotation instead.
}

# ─── MSK (Kafka) ──────────────────────────────────────────────────────────────

variable "msk_instance_type" {
  description = "MSK broker instance type"
  type        = string
  default     = "kafka.t3.small"  # dev; kafka.m5.large in prod
}

variable "msk_broker_count" {
  description = "Number of MSK brokers (must be a multiple of AZ count)"
  type        = number
  default     = 1  # dev (single-broker); 3 in prod (one per AZ)
}

variable "msk_kafka_version" {
  description = "Kafka version for MSK"
  type        = string
  default     = "3.6.0"
}

# ─── Secrets ──────────────────────────────────────────────────────────────────

variable "jwt_signing_key" {
  description = "JWT HS256 signing key for api-gateway demo mode"
  type        = string
  sensitive   = true
}

variable "encryption_passphrase" {
  description = "AES-256-GCM passphrase for customer-service nationalId encryption"
  type        = string
  sensitive   = true
}

variable "fraud_decision_hmac_secret" {
  description = "HMAC-SHA256 shared secret for transfer-service/fraud-ai-service callback"
  type        = string
  sensitive   = true
}

# ─── Route53 / ACM ────────────────────────────────────────────────────────────

variable "domain_name" {
  description = "Root domain name (e.g. banking.example.com)"
  type        = string
  default     = "banking.example.com"
}

variable "route53_zone_id" {
  description = "Route53 hosted zone ID for the domain"
  type        = string
  default     = ""  # leave empty to skip DNS/cert creation in dev
}
