output "alb_controller_role_arn" { value = aws_iam_role.alb_controller.arn }
output "alb_dns_name" {
  description = "ALB DNS name (null if route53_zone_id is empty and no ingress exists yet)"
  value       = try(helm_release.alb_controller.metadata[0].name, "")
}
output "acm_certificate_arn" {
  value = var.route53_zone_id != "" ? aws_acm_certificate.api[0].arn : ""
}
