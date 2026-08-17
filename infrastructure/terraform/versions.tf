terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Remote state in S3 + DynamoDB lock table.
  # Replace bucket/table names with your real values before running.
  # Create the bucket and table manually (chicken-and-egg: Terraform can't
  # manage its own backend bucket) or use terraform-bootstrap scripts.
  backend "s3" {
    bucket         = "banking-platform-tfstate"
    key            = "banking/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "banking-platform-tflock"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "banking-ai-platform"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# Kubernetes + Helm providers are configured after EKS is created.
# They use the cluster endpoint and CA cert from the EKS module output.
provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
  token                  = module.eks.cluster_token
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
    token                  = module.eks.cluster_token
  }
}
