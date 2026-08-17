data "aws_caller_identity" "current" {}

# ─── IAM role for AWS Load Balancer Controller (IRSA) ────────────────────────

locals {
  oidc_provider = replace(var.eks_oidc_issuer, "https://", "")
}

resource "aws_iam_role" "alb_controller" {
  name = "${var.name_prefix}-alb-controller"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/${local.oidc_provider}" }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_provider}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "alb_controller" {
  role       = aws_iam_role.alb_controller.name
  policy_arn = aws_iam_policy.alb_controller.arn
}

# IAM policy for ALB controller (standard AWS-recommended policy)
resource "aws_iam_policy" "alb_controller" {
  name        = "${var.name_prefix}-alb-controller-policy"
  description = "IAM policy for AWS Load Balancer Controller"

  # The full policy JSON from https://raw.githubusercontent.com/kubernetes-sigs/
  # aws-load-balancer-controller/main/docs/install/iam_policy.json
  # Abbreviated here for readability — use the full policy in production.
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ec2:DescribeVpcs", "ec2:DescribeSubnets", "ec2:DescribeSecurityGroups",
                    "ec2:DescribeInstances", "ec2:DescribeInternetGateways",
                    "elasticloadbalancing:*", "wafv2:*", "waf-regional:*",
                    "shield:*", "iam:CreateServiceLinkedRole",
                    "cognito-idp:DescribeUserPoolClient",
                    "acm:ListCertificates", "acm:DescribeCertificate",
                    "ec2:AuthorizeSecurityGroupIngress", "ec2:RevokeSecurityGroupIngress",
                    "ec2:CreateSecurityGroup", "ec2:CreateTags", "ec2:DeleteTags",
                    "ec2:DeleteSecurityGroup"]
        Resource = "*"
      }
    ]
  })
}

# ─── AWS Load Balancer Controller (Helm) ─────────────────────────────────────

resource "helm_release" "alb_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  version    = "1.8.1"
  namespace  = "kube-system"

  set {
    name  = "clusterName"
    value = var.eks_cluster_name
  }
  set {
    name  = "serviceAccount.create"
    value = "true"
  }
  set {
    name  = "serviceAccount.name"
    value = "aws-load-balancer-controller"
  }
  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = aws_iam_role.alb_controller.arn
  }
  set {
    name  = "region"
    value = var.aws_region
  }
  set {
    name  = "vpcId"
    value = var.vpc_id
  }
}

# ─── ACM Certificate (optional — skipped if route53_zone_id is empty) ────────

resource "aws_acm_certificate" "api" {
  count             = var.route53_zone_id != "" ? 1 : 0
  domain_name       = "api.${var.domain_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "cert_validation" {
  for_each = var.route53_zone_id != "" ? {
    for dvo in aws_acm_certificate.api[0].domain_validation_options :
    dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  } : {}

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = var.route53_zone_id
}

resource "aws_acm_certificate_validation" "api" {
  count                   = var.route53_zone_id != "" ? 1 : 0
  certificate_arn         = aws_acm_certificate.api[0].arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}
