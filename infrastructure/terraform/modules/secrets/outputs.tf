output "secret_arns" {
  description = "Map of secret name -> ARN (attach to IRSA policies)"
  sensitive   = true
  value = merge(
    {
      "jwt-signing-key"            = aws_secretsmanager_secret.jwt_signing_key.arn
      "encryption-passphrase"      = aws_secretsmanager_secret.encryption_passphrase.arn
      "fraud-decision-hmac-secret" = aws_secretsmanager_secret.fraud_hmac.arn
    },
    { for k, v in aws_secretsmanager_secret.rds : "${k}-db-password" => v.arn }
  )
}
