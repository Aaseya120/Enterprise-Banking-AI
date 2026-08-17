resource "aws_ecr_repository" "services" {
  for_each             = toset(var.services)
  name                 = "${var.name_prefix}/${each.key}"
  image_tag_mutability = "IMMUTABLE"  # prevent tag overwriting (audit / rollback safety)

  image_scanning_configuration {
    scan_on_push = true   # ECR basic scanning (upgrade to ECR Enhanced for CVE data)
  }

  encryption_configuration {
    encryption_type = "AES256"   # AWS-managed encryption at rest
  }

  tags = {
    Name    = "${var.name_prefix}/${each.key}"
    Service = each.key
  }
}

# Lifecycle policy: keep the last 30 images per repo; clean up old tags
# to avoid unbounded storage costs.
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 30 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 30
        }
        action = { type = "expire" }
      }
    ]
  })
}
