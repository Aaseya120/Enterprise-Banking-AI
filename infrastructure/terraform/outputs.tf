output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "eks_cluster_name" {
  description = "EKS cluster name (use with: aws eks update-kubeconfig --name <value>)"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS API server endpoint"
  value       = module.eks.cluster_endpoint
}

output "ecr_repository_urls" {
  description = "Map of service name -> ECR repository URL for use in CI/CD image push"
  value       = module.ecr.repository_urls
}

output "msk_bootstrap_brokers" {
  description = "MSK Kafka bootstrap broker string (plaintext) — set as SPRING_KAFKA_BOOTSTRAP_SERVERS"
  value       = module.msk.bootstrap_brokers
}

output "msk_bootstrap_brokers_tls" {
  description = "MSK Kafka bootstrap broker string (TLS)"
  value       = module.msk.bootstrap_brokers_tls
}

output "rds_endpoints" {
  description = "Map of service name -> RDS endpoint hostname"
  value = {
    for name, mod in module.rds : name => mod.endpoint
  }
}

output "secrets_arns" {
  description = "Map of secret name -> Secrets Manager ARN (for IRSA policy attachment)"
  value       = module.secrets.secret_arns
  sensitive   = true
}

output "alb_dns_name" {
  description = "ALB DNS name — point your domain's CNAME/ALIAS here if not using Route53"
  value       = module.alb_ingress.alb_dns_name
}
