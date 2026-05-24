resource "aws_sns_topic" "notifications" {
  name = "${var.project_name}-notifications"
}

resource "aws_security_group" "notification_alb" {
  name        = "${var.project_name}-notification-alb-sg"
  description = "Notification service ALB security group"
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

resource "aws_security_group" "notification_ecs" {
  name        = "${var.project_name}-notification-ecs-sg"
  description = "Notification service ECS security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Traffic from notification ALB"
    from_port       = var.notification_container_port
    to_port         = var.notification_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.notification_alb.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "notification_db" {
  name        = "${var.project_name}-notification-db-sg"
  description = "Notification service database security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Postgres from notification ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.notification_ecs.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "notification" {
  name       = "${var.project_name}-notification-db-subnets"
  subnet_ids = aws_subnet.public[*].id

  tags = {
    Name = "${var.project_name}-notification-db-subnets"
  }
}

resource "aws_db_instance" "notification" {
  identifier             = "${var.project_name}-notification-db"
  engine                 = "postgres"
  instance_class         = "db.t3.micro"
  allocated_storage      = 5
  db_name                = var.notification_db_name
  username               = var.notification_db_username
  password               = var.notification_db_password
  vpc_security_group_ids = [aws_security_group.notification_db.id]
  db_subnet_group_name   = aws_db_subnet_group.notification.name
  publicly_accessible    = true
  skip_final_snapshot    = true
}

resource "aws_lb" "notification" {
  name               = "${var.project_name}-notification-alb"
  load_balancer_type = "application"
  subnets            = aws_subnet.public[*].id
  security_groups    = [aws_security_group.notification_alb.id]

  tags = {
    Name = "${var.project_name}-notification-alb"
  }
}

resource "aws_lb_target_group" "notification" {
  name        = "${var.project_name}-notification-tg"
  port        = var.notification_container_port
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

resource "aws_lb_listener" "notification_http" {
  load_balancer_arn = aws_lb.notification.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.notification.arn
  }
}

resource "aws_ecs_cluster" "notification" {
  name = "${var.project_name}-notification-cluster"
}

resource "aws_cloudwatch_log_group" "notification" {
  name              = "/ecs/${var.project_name}-notification-service"
  retention_in_days = 14
}

locals {
  notification_environment = [
    {
      name  = "SPRING_DATASOURCE_URL"
      value = "jdbc:postgresql://${aws_db_instance.notification.address}:${aws_db_instance.notification.port}/${var.notification_db_name}"
    },
    {
      name  = "SPRING_DATASOURCE_USERNAME"
      value = var.notification_db_username
    },
    {
      name  = "SPRING_DATASOURCE_PASSWORD"
      value = var.notification_db_password
    },
    {
      name  = "SPRING_JPA_HIBERNATE_DDL_AUTO"
      value = "validate"
    },
    {
      name  = "SPRING_FLYWAY_ENABLED"
      value = "true"
    },
    {
      name  = "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI"
      value = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.auth.id}"
    },
    {
      name  = "AWS_REGION"
      value = var.aws_region
    },
    {
      name  = "AWS_SNS_TOPIC_ARN"
      value = aws_sns_topic.notifications.arn
    },
    {
      name  = "CHAT_SERVICE_BASE_URL"
      value = "http://${aws_lb.chat.dns_name}"
    }
  ]
}

resource "aws_sns_topic_subscription" "notification" {
  topic_arn = aws_sns_topic.notifications.arn
  protocol  = "http"
  endpoint  = "http://${aws_lb.notification.dns_name}/notifications/sns"

  depends_on = [aws_lb_listener.notification_http]
}

resource "aws_ecs_task_definition" "notification" {
  family                   = "${var.project_name}-notification-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name      = "notification-service"
      image     = var.notification_container_image
      essential = true
      portMappings = [
        {
          containerPort = var.notification_container_port
          hostPort      = var.notification_container_port
          protocol      = "tcp"
        }
      ]
      environment = local.notification_environment
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.notification.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "notification" {
  name            = "${var.project_name}-notification-service"
  cluster         = aws_ecs_cluster.notification.id
  task_definition = aws_ecs_task_definition.notification.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.notification_ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.notification.arn
    container_name   = "notification-service"
    container_port   = var.notification_container_port
  }

  depends_on = [aws_lb_listener.notification_http]
}

resource "aws_appautoscaling_target" "notification_ecs" {
  max_capacity       = var.max_tasks
  min_capacity       = var.min_tasks
  resource_id        = "service/${aws_ecs_cluster.notification.name}/${aws_ecs_service.notification.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "notification_ecs_cpu" {
  name               = "${var.project_name}-notification-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.notification_ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.notification_ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.notification_ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value = var.cpu_utilization_target

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    scale_in_cooldown  = 120
    scale_out_cooldown = 60
  }
}
