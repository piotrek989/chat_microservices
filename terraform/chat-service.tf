resource "aws_security_group" "chat_alb" {#punkt wejscia do zapytan HTTP do chat-service, kontroluje ruch sieciowy do ALB, pozwalajac na dostep z internetu
  name        = "${var.project_name}-chat-alb-sg" #nazwa SG dla ALB
  description = "Chat service ALB security group"
  vpc_id      = aws_vpc.main.id #powiazanie z VPC

  ingress {
    description = "HTTP from internet"
    from_port   = 80 #publiczny HTTP
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

resource "aws_security_group" "chat_ecs" {#firewalle kontrolujące ruch sieciowy do i z uruchomionych kontenerów
  name        = "${var.project_name}-chat-ecs-sg" #SG dla zadan ECS
  description = "Chat service ECS security group"
  vpc_id      = aws_vpc.main.id #powiazanie z VPC

  ingress {
    description     = "Traffic from chat ALB"
    from_port       = var.chat_container_port #port aplikacji w kontenerze
    to_port         = var.chat_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.chat_alb.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]#moze łaczyc sie z kazdym IP w internecie
  }
}

resource "aws_security_group" "chat_db" {
  name        = "${var.project_name}-chat-db-sg" #SG dla bazy danych
  description = "Chat service database security group"
  vpc_id      = aws_vpc.main.id #powiazanie z VPC

  ingress {
    description     = "Postgres from chat ECS"
    from_port       = 5432 #PostgreSQL
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.chat_ecs.id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "chat" {
  name       = "${var.project_name}-chat-db-subnets" #podsiec dla RDS
  subnet_ids = aws_subnet.public[*].id

  tags = {
    Name = "${var.project_name}-chat-db-subnets"
  }
}

resource "aws_db_instance" "chat" {
  identifier             = "${var.project_name}-chat-db" #nazwa instancji RDS
  engine                 = "postgres" #silnik bazy danych
  instance_class         = "db.t3.micro" #minimalny rozmiar instancji
  allocated_storage      = 5 #pojemnosc dysku (GB)
  db_name                = var.chat_db_name #nazwa bazy
  username               = var.chat_db_username #uzytkownik bazy
  password               = var.chat_db_password #haslo bazy
  vpc_security_group_ids = [aws_security_group.chat_db.id] #kontrola dostepu do RDS
  db_subnet_group_name   = aws_db_subnet_group.chat.name #podsieci dla RDS
  publicly_accessible    = true #uproczony dostep w laboratorium
  skip_final_snapshot    = true #brak snapshotu przy usunieciu
}

resource "aws_lb" "chat" {
  name               = "${var.project_name}-chat-alb" #ALB dla chat-service
  load_balancer_type = "application"
  subnets            = aws_subnet.public[*].id
  security_groups    = [aws_security_group.chat_alb.id]#przypisanie grupy zabezpieczen do zasobu ALB

  tags = {
    Name = "${var.project_name}-chat-alb"
  }
}

resource "aws_lb_target_group" "chat" {
  name        = "${var.project_name}-chat-tg" #target group dla ECS
  port        = var.chat_container_port #port aplikacji
  protocol    = "HTTP" #ruch HTTP do kontenera
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

resource "aws_lb_listener" "chat_http" {
  load_balancer_arn = aws_lb.chat.arn #listener na ALB
  port              = 80 #HTTP na zewnatrz
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.chat.arn
  }
}

resource "aws_ecs_cluster" "chat" {
  name = "${var.project_name}-chat-cluster" #klaster ECS dla chat-service
}

resource "aws_cloudwatch_log_group" "chat" {
  name              = "/ecs/${var.project_name}-chat-service" #logi kontenera
  retention_in_days = 14
}

locals {
  chat_environment = [
    {
      name  = "AWS_REGION"
      value = var.aws_region
    },
    {
      name  = "AWS_SNS_TOPIC_ARN"
      value = aws_sns_topic.notifications.arn
    },
    {
      name  = "SPRING_DATASOURCE_URL"
      value = "jdbc:postgresql://${aws_db_instance.chat.address}:${aws_db_instance.chat.port}/${var.chat_db_name}" #adres RDS
    },
    {
      name  = "SPRING_DATASOURCE_USERNAME"
      value = var.chat_db_username
    },
    {
      name  = "SPRING_DATASOURCE_PASSWORD"
      value = var.chat_db_password
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
      value = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.auth.id}" #issuer dla JWT
    }
  ]
}

resource "aws_ecs_task_definition" "chat" {
  family                   = "${var.project_name}-chat-service" #rodzina taskow
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu #CPU dla zadania
  memory                   = var.task_memory #pamiec dla zadania
  execution_role_arn       = data.aws_iam_role.lab_role.arn #rola do uruchomienia taska
  task_role_arn            = data.aws_iam_role.lab_role.arn #rola runtime aplikacji

  container_definitions = jsonencode([
    {
      name      = "chat-service"
      image     = var.chat_container_image #obraz aplikacji
      essential = true
      portMappings = [
        {
          containerPort = var.chat_container_port
          hostPort      = var.chat_container_port
          protocol      = "tcp"
        }
      ]
      environment = local.chat_environment #zmienne dla Spring Boot
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.chat.name #grupa logow
          awslogs-region        = var.aws_region #region logow
          awslogs-stream-prefix = "ecs" #prefiks strumieni
        }
      }
    }
  ])
}

resource "aws_ecs_service" "chat" {
  name            = "${var.project_name}-chat-service" #nazwa serwisu ECS
  cluster         = aws_ecs_cluster.chat.id
  task_definition = aws_ecs_task_definition.chat.arn
  desired_count   = var.desired_count #startowa liczba taskow
  launch_type     = "FARGATE" #uruchamianie na Fargate

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.chat_ecs.id]
    assign_public_ip = true #publiczny adres dla taskow
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.chat.arn #powiazanie z ALB
    container_name   = "chat-service"
    container_port   = var.chat_container_port
  }

  depends_on = [aws_lb_listener.chat_http]
}

resource "aws_appautoscaling_target" "chat_ecs" {
  max_capacity       = var.max_tasks #limit skalowania w gore
  min_capacity       = var.min_tasks #minimum 2 taski
  resource_id        = "service/${aws_ecs_cluster.chat.name}/${aws_ecs_service.chat.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "chat_ecs_cpu" {
  name               = "${var.project_name}-chat-cpu-scaling" #skalowanie po CPU
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.chat_ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.chat_ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.chat_ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value = var.cpu_utilization_target #docelowe obciazenie CPU

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    scale_in_cooldown  = 120
    scale_out_cooldown = 60
  }
}
