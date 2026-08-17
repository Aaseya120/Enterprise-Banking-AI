variable "name_prefix"                { type = string }
variable "kafka_version"              { type = string }
variable "broker_instance_type"       { type = string }
variable "broker_count"               { type = number }
variable "subnet_ids"                 { type = list(string) }
variable "vpc_id"                     { type = string }
variable "allowed_security_group_ids" { type = list(string) }
