resource "aws_s3_bucket" "file" {
  bucket        = var.file_bucket_name
  force_destroy = true

  tags = {
    Name = "${var.project_name}-file-bucket"
  }
}

resource "aws_s3_bucket_public_access_block" "file" {
  bucket = aws_s3_bucket.file.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "file" {
  name         = var.file_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "fileId"

  attribute {
    name = "fileId"
    type = "S"
  }

  attribute {
    name = "chatId"
    type = "S"
  }

  global_secondary_index {
    name            = "chatId-index"
    hash_key        = "chatId"
    projection_type = "ALL"
  }

  tags = {
    Name = "${var.project_name}-file-metadata"
  }
}

resource "aws_security_group" "file_alb" {
  name        = "${var.project_name}-file-alb-sg"
  description = "File service ALB security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "file_ecs" {
  name        = "${var.project_name}-file-ecs-sg"
  description = "File service ECS security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Traffic from file ALB"
    from_port       = var.file_container_port
    to_port         = var.file_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.file_alb.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "file" {
  name               = "${var.project_name}-file-alb"
  load_balancer_type = "application"
  subnets            = aws_subnet.public[*].id
  security_groups    = [aws_security_group.file_alb.id]

  tags = {
    Name = "${var.project_name}-file-alb"
  }
}

resource "aws_lb_target_group" "file" {
  name        = "${var.project_name}-file-tg"
  port        = var.file_container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "file_http" {
  load_balancer_arn = aws_lb.file.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.file.arn
  }
}

resource "aws_ecs_cluster" "file" {
  name = "${var.project_name}-file-cluster"
}

resource "aws_cloudwatch_log_group" "file" {
  name              = "/ecs/${var.project_name}-file-service"
  retention_in_days = 14
}

locals {
  file_environment = [
    {
      name  = "AWS_REGION"
      value = var.aws_region
    },
    {
      name  = "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI"
      value = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.auth.id}" #issuer dla JWT
    },
    {
      name  = "AWS_SNS_TOPIC_ARN"
      value = aws_sns_topic.notifications.arn
    },
    {
      name  = "FILE_BUCKET_NAME"
      value = aws_s3_bucket.file.bucket
    },
    {
      name  = "FILE_TABLE_NAME"
      value = aws_dynamodb_table.file.name
    }
  ]
}

resource "aws_ecs_task_definition" "file" {
  family                   = "${var.project_name}-file-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name      = "file-service"
      image     = var.file_container_image
      essential = true
      portMappings = [
        {
          containerPort = var.file_container_port
          hostPort      = var.file_container_port
          protocol      = "tcp"
        }
      ]
      environment = local.file_environment
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.file.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "file" {
  name            = "${var.project_name}-file-service"
  cluster         = aws_ecs_cluster.file.id
  task_definition = aws_ecs_task_definition.file.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.file_ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.file.arn
    container_name   = "file-service"
    container_port   = var.file_container_port
  }

  depends_on = [aws_lb_listener.file_http]
}

resource "aws_appautoscaling_target" "file_ecs" {
  max_capacity       = var.max_tasks
  min_capacity       = var.min_tasks
  resource_id        = "service/${aws_ecs_cluster.file.name}/${aws_ecs_service.file.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "file_ecs_cpu" {
  name               = "${var.project_name}-file-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.file_ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.file_ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.file_ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value = var.cpu_utilization_target

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    scale_in_cooldown  = 120
    scale_out_cooldown = 60
  }
}
