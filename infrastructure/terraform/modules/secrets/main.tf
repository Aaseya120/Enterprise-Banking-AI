# Secrets Manager entries for all platform credentials.
# Each secret is a separate resource (not one big JSON blob) so:
# 1. IAM policies can grant access per-secret (customer-service reads only its own)
# 2. Rotation can be configured independently per secret
# 3. Secret drift is isolated — one changed value doesn't force rotation of all

resource "aws_secretsmanager_secret" "jwt_signing_key" {
  name                    = "${var.name_prefix}/api-gateway/jwt-signing-key"
  description             = "api-gateway HS256 JWT signing key (demo mode)"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "jwt_signing_key" {
  secret_id     = aws_secretsmanager_secret.jwt_signing_key.id
  secret_string = var.jwt_signing_key
}

resource "aws_secretsmanager_secret" "encryption_passphrase" {
  name                    = "${var.name_prefix}/customer-service/encryption-passphrase"
  description             = "AES-256-GCM passphrase for customer-service nationalId encryption"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "encryption_passphrase" {
  secret_id     = aws_secretsmanager_secret.encryption_passphrase.id
  secret_string = var.encryption_passphrase
}

resource "aws_secretsmanager_secret" "fraud_hmac" {
  name                    = "${var.name_prefix}/shared/fraud-decision-hmac-secret"
  description             = "HMAC-SHA256 shared secret for transfer-service/fraud-ai-service saga callback"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "fraud_hmac" {
  secret_id     = aws_secretsmanager_secret.fraud_hmac.id
  secret_string = var.fraud_decision_hmac_secret
}

# One Secrets Manager secret per RDS service (DB password)
resource "aws_secretsmanager_secret" "rds" {
  for_each                = var.rds_endpoints
  name                    = "${var.name_prefix}/${each.key}-service/db-password"
  description             = "PostgreSQL password for ${each.key}-service"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "rds" {
  for_each  = var.rds_endpoints
  secret_id = aws_secretsmanager_secret.rds[each.key].id
  # Store as JSON so External Secrets Operator can extract individual fields
  secret_string = jsonencode({
    password = var.rds_master_password
    host     = each.value.endpoint
    dbname   = each.value.db_name
    username = "postgres"
    port     = 5432
  })
}
