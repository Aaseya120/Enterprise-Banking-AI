resource "aws_db_subnet_group" "this" {
  name        = "${var.name_prefix}-db-subnet-group"
  subnet_ids  = var.subnet_ids
  description = "Subnet group for ${var.name_prefix} RDS"
  tags        = { Name = "${var.name_prefix}-db-subnet-group" }
}

resource "aws_security_group" "rds" {
  name        = "${var.name_prefix}-rds-sg"
  description = "Allow PostgreSQL from EKS nodes only"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
    description     = "PostgreSQL from EKS worker nodes"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.name_prefix}-rds-sg" }
}

resource "aws_db_parameter_group" "postgres16" {
  name   = "${var.name_prefix}-pg16-params"
  family = "postgres16"

  # log_connections and log_disconnections help with audit trails
  parameter {
    name  = "log_connections"
    value = "1"
  }
  parameter {
    name  = "log_disconnections"
    value = "1"
  }
  # Enforce SSL connections — services connect with ssl=require in the JDBC URL
  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }
}

resource "aws_db_instance" "this" {
  identifier              = var.name_prefix
  engine                  = "postgres"
  engine_version          = "16.3"
  instance_class          = var.instance_class
  allocated_storage       = 20     # GB (auto-scaled by storage_autoscaling_enabled)
  max_allocated_storage   = 100    # upper bound for autoscaling
  storage_type            = "gp3"
  storage_encrypted       = true   # encryption at rest (AWS-managed KMS key)
  db_name                 = var.db_name
  username                = "postgres"
  password                = var.master_password
  multi_az                = var.multi_az
  deletion_protection     = var.deletion_protection
  skip_final_snapshot     = !var.deletion_protection  # always snapshot in prod
  final_snapshot_identifier = "${var.name_prefix}-final-snapshot"
  backup_retention_period = 7      # days (set 30 in prod)
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"
  db_subnet_group_name    = aws_db_subnet_group.this.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  parameter_group_name    = aws_db_parameter_group.postgres16.name
  performance_insights_enabled = true  # free tier available
  monitoring_interval     = 60         # Enhanced Monitoring every 60s

  tags = { Name = var.name_prefix }
}
