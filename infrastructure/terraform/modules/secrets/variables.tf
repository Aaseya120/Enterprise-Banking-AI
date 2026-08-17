variable "name_prefix"                 { type = string }
variable "jwt_signing_key"             { type = string; sensitive = true }
variable "encryption_passphrase"       { type = string; sensitive = true }
variable "fraud_decision_hmac_secret"  { type = string; sensitive = true }
variable "rds_master_password"         { type = string; sensitive = true }
variable "rds_endpoints" {
  type = map(object({ endpoint = string; db_name = string }))
}
