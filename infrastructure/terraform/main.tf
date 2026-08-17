# Root main.tf — wires all modules together.
# Every module output is documented so dependent modules can reference them
# without needing to know the module internals.

locals {
  name_prefix = "${var.project}-${var.environment}"

  # The 13 stateful services, each getting its own RDS instance.
  # name = used as resource name suffix and database name (with _ replacing -)
  rds_services = [
    { name = "customer",              db = "customer_db",         port = 8086 },
    { name = "account",               db = "account_db",          port = 8081 },
    { name = "transfer",              db = "transfer_db",         port = 8084 },
    { name = "payment",               db = "payment_db",          port = 8087 },
    { name = "transaction",           db = "transaction_db",      port = 8088 },
    { name = "card",                  db = "card_db",             port = 8089 },
    { name = "loan",                  db = "loan_db",             port = 8090 },
    { name = "notification",          db = "notification_db",     port = 8091 },
    { name = "audit",                 db = "audit_db",            port = 8092 },
    { name = "fraud",                 db = "fraud_db",            port = 8093 },
    { name = "knowledge",             db = "knowledge_db",        port = 8094 },
    { name = "document-intelligence", db = "document_intel_db",  port = 8095 },
    { name = "report",                db = "report_db",           port = 8096 },
  ]

  # All 19 deployable services needing ECR repositories
  ecr_services = [
    "api-gateway",
    "ai-orchestrator-service",
    "account-service",
    "rag-service",
    "mcp-gateway-service",
    "transfer-service",
    "fraud-ai-service",
    "customer-service",
    "payment-service",
    "transaction-service",
    "card-service",
    "loan-service",
    "notification-service",
    "audit-service",
    "fraud-service",
    "knowledge-service",
    "document-intelligence-service",
    "report-insight-service",
  ]
}

# ─── VPC ──────────────────────────────────────────────────────────────────────

module "vpc" {
  source = "./modules/vpc"

  name_prefix          = local.name_prefix
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}

# ─── EKS ──────────────────────────────────────────────────────────────────────

module "eks" {
  source = "./modules/eks"

  name_prefix              = local.name_prefix
  cluster_version          = var.eks_cluster_version
  vpc_id                   = module.vpc.vpc_id
  private_subnet_ids       = module.vpc.private_subnet_ids
  system_node_instance_type   = var.eks_system_node_type
  service_node_instance_type  = var.eks_service_node_type
  service_node_min_size       = var.eks_service_node_min
  service_node_max_size       = var.eks_service_node_max
  service_node_desired_size   = var.eks_service_node_desired
}

# ─── RDS (one instance per stateful service) ──────────────────────────────────

module "rds" {
  source   = "./modules/rds"
  for_each = { for s in local.rds_services : s.name => s }

  name_prefix        = "${local.name_prefix}-${each.key}"
  db_name            = each.value.db
  instance_class     = var.rds_instance_class
  multi_az           = var.rds_multi_az
  deletion_protection = var.rds_deletion_protection
  master_password    = var.rds_master_password
  vpc_id             = module.vpc.vpc_id
  subnet_ids         = module.vpc.private_subnet_ids
  # Allow access only from EKS nodes
  allowed_security_group_ids = [module.eks.node_security_group_id]
}

# ─── MSK (managed Kafka) ──────────────────────────────────────────────────────

module "msk" {
  source = "./modules/msk"

  name_prefix        = local.name_prefix
  kafka_version      = var.msk_kafka_version
  broker_instance_type = var.msk_instance_type
  broker_count       = var.msk_broker_count
  subnet_ids         = module.vpc.private_subnet_ids
  vpc_id             = module.vpc.vpc_id
  allowed_security_group_ids = [module.eks.node_security_group_id]
}

# ─── ECR (one repo per deployable service) ────────────────────────────────────

module "ecr" {
  source = "./modules/ecr"

  services    = local.ecr_services
  name_prefix = local.name_prefix
  environment = var.environment
}

# ─── Secrets Manager ──────────────────────────────────────────────────────────

module "secrets" {
  source = "./modules/secrets"

  name_prefix = local.name_prefix

  jwt_signing_key            = var.jwt_signing_key
  encryption_passphrase      = var.encryption_passphrase
  fraud_decision_hmac_secret = var.fraud_decision_hmac_secret

  # DB passwords: one secret per service (supports per-service rotation)
  rds_endpoints = {
    for name, mod in module.rds : name => {
      endpoint = mod.endpoint
      db_name  = mod.db_name
    }
  }
  rds_master_password = var.rds_master_password
}

# ─── ALB Ingress Controller (Helm release) ────────────────────────────────────

module "alb_ingress" {
  source = "./modules/alb-ingress"

  name_prefix        = local.name_prefix
  eks_cluster_name   = module.eks.cluster_name
  eks_oidc_issuer    = module.eks.oidc_issuer_url
  vpc_id             = module.vpc.vpc_id
  domain_name        = var.domain_name
  route53_zone_id    = var.route53_zone_id
  aws_region         = var.aws_region
}
