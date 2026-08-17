variable "name_prefix"                { type = string }
variable "db_name"                    { type = string }
variable "instance_class"             { type = string }
variable "multi_az"                   { type = bool }
variable "deletion_protection"        { type = bool }
variable "master_password"            { type = string; sensitive = true }
variable "vpc_id"                     { type = string }
variable "subnet_ids"                 { type = list(string) }
variable "allowed_security_group_ids" { type = list(string) }
