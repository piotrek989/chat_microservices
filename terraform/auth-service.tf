data "aws_availability_zones" "available" {}#pobranie automatycznie listy dostępnych stref 
#dostępności w regionie, aby przypisać podsieci do różnych AZ dla lepszej dostępności i odporności.

resource "aws_cognito_user_pool" "auth" {
  # Cognito user pool for email-based authentication.
  name = "${var.project_name}-auth-user-pool"

  auto_verified_attributes  = ["email"]
  #wyslanie na maila kodu weryfikacyjnego

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
    require_uppercase = true
  }
}

resource "aws_cognito_user_pool_client" "auth" {
  # Cognito app client used by the auth-service.
  name         = "${var.project_name}-auth-client"
  user_pool_id = aws_cognito_user_pool.auth.id

  generate_secret = false #nie potrzebujemy secretu, bo nasza aplikacja
  # będzie publiczna (frontend) i nie będzie bezpiecznie przechowywać sekretu

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]
}
# ALLOW_USER_PASSWORD_AUTH: Klasyczne logowanie (e-mail + hasło) przesyłane bezpośrednio.
# ALLOW_REFRESH_TOKEN_AUTH: Pozwala na uzyskanie nowego access_token bez ponownego wpisywania hasła.
# ALLOW_USER_SRP_AUTH: Bezpieczniejszy protokół logowania (Secure Remote Password), 
#który nie wysyła hasła otwartym tekstem przez sieć.

#ogolnie vpc pozwaka na uruchamianie kontenerów w odizolowanym, bezpiecznym środowisku,
# a Cognito zapewnia łatwe zarządzanie użytkownikami i uwierzytelnianiem, co jest kluczowe dla auth-service.
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr # zakres adresów IP dla VPC
  enable_dns_hostnames = true # włącz rozpoznawanie nazw hostów
  enable_dns_support   = true # włącz obsługę DNS w VPC

  tags = {
    Name = "${var.project_name}-vpc"
  }
} 
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = {
    Name = "${var.project_name}-igw"
  }
}

# tworzymy publiczne podsieci w różnych strefach dostępności
resource "aws_subnet" "public" { # zasób podsieci publicznej
  # Używamy listy podsieci, a nie głównego CIDR-u VPC
  count                   = length(var.public_subnet_cidrs) # iteruj po każdej podsieci
  vpc_id                  = aws_vpc.main.id # przypisz podsieć do VPC
  cidr_block              = var.public_subnet_cidrs[count.index] # zakres adresów podsieci
  availability_zone       = data.aws_availability_zones.available.names[count.index] # rozłóż po AZ
  map_public_ip_on_launch = true # przydzielaj publiczne IP automatycznie

  tags = {
    Name = "${var.project_name}-public-${count.index + 1}" 
  }
}
# tworzymy tabelę routingu dla podsieci publicznych
resource "aws_route_table" "public" { # zasób tabeli routingu
  vpc_id = aws_vpc.main.id # przypisz tabelę do VPC
# definiuj trasę do internetu
  route { # konfiguracja trasy sieciowej
    cidr_block = "0.0.0.0/0" # ruta domyślna dla całego internetu
    gateway_id = aws_internet_gateway.main.id # skieruj do bramy
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  } 
}
# łączymy tabelę routingu z każdą podsiecią publiczną
resource "aws_route_table_association" "public" { # asocjacja tabeli routingu
  count          = length(aws_subnet.public) # iteruj po każdej podsieci
  subnet_id      = aws_subnet.public[count.index].id # weź ID podsieci
  route_table_id = aws_route_table.public.id # przypisz naszą tabelę
}
# grupa bezpieczeństwa dla load balancera (ALB)
resource "aws_security_group" "alb" { # zasób grupy bezpieczeństwa
  name        = "${var.project_name}-alb-sg" # nazwa grupy bezpieczeństwa
  description = "ALB security group" # opis grupy
  vpc_id      = aws_vpc.main.id # przypisz do VPC

  ingress { # konfiguracja ruchu wejściowego
    description = "HTTP from internet" # opis reguły ingress
    from_port   = 80 # port początkowy HTTP
    to_port     = 80 # port końcowy HTTP
    protocol    = "tcp" # protokół TCP
    cidr_blocks = ["0.0.0.0/0"] # zezwól z całego internetu
  }

  egress { # konfiguracja ruchu wychodzącego
    description = "All outbound" # opis reguły egress
    from_port   = 0 # dowolny port początkowy
    to_port     = 0 # dowolny port końcowy
    protocol    = "-1" # dowolny protokół
    cidr_blocks = ["0.0.0.0/0"] # zezwól do całego internetu
  }
}
# grupa bezpieczeństwa dla kontenerów ECS
resource "aws_security_group" "ecs" {# wirtualne firewalle kontrolujące ruch sieciowy
 # do i z zasobów ECS, zapewniając bezpieczną komunikację między ALB a zadaniami ECS.
  name        = "${var.project_name}-ecs-sg" # nazwa grupy bezpieczeństwa
  description = "ECS service security group" # opis grupy
  vpc_id      = aws_vpc.main.id # przypisz do VPC
# reguła ruchu przychodzącego
  ingress { # konfiguracja ruchu wejściowego
    description     = "Traffic from ALB" # ruch z load balancera
    from_port       = var.auth_container_port # port aplikacji
    to_port         = var.auth_container_port # ten sam port
    protocol        = "tcp" # protokół TCP
    security_groups = [aws_security_group.alb.id] # tylko z ALB
  } # koniec reguły ingress
# reguła ruchu wychodzącego
  egress { # konfiguracja ruchu wychodzącego
    description = "All outbound" # opis reguły egress
    from_port   = 0 # dowolny port początkowy
    to_port     = 0 # dowolny port końcowy
    protocol    = "-1" # dowolny protokół
    cidr_blocks = ["0.0.0.0/0"] # zezwól do całego internetu
  } # koniec reguły egress
} 
# Application Load Balancer - rozkładaj ruch HTTP po kontenerach ECS
resource "aws_lb" "auth" { # zasób load balancera aplikacyjnego
  # Application Load Balancer for routing external traffic to ECS tasks.
  name               = "${var.project_name}-auth-alb" # nazwa load balancera
  load_balancer_type = "application" # typ load balancera
  subnets            = aws_subnet.public[*].id # umieść w subnetach publicznych
  security_groups    = [aws_security_group.alb.id] # przypisz grupę bezpieczeństwa

  tags = {
    Name = "${var.project_name}-auth-alb"
  } 
}
# grupa docelowa - gdzie load balancer wysyła ruch generowany przez userow
resource "aws_lb_target_group" "auth" { # zasób grupy docelowej
  # Target group used by the ALB to forward traffic to Fargate tasks.
  name        = "${var.project_name}-auth-tg" # nazwa grupy docelowej
  port        = var.auth_container_port# wysyła na ten port do kontenera
  #ALB nasłuchuje na porcie 80 i przekierowuje do tego portu
  protocol    = "HTTP" # protokół komunikacji
  target_type = "ip" # typ celu - IP adres
  vpc_id      = aws_vpc.main.id # przypisz do VPC

  health_check { # monitoring zdrowia kontenerów
    path                = "/health" # endpoint do sprawdzania
    matcher             = "200" # akceptuj status HTTP 200
    interval            = 30 # sprawdzaj co 30 sekund
    healthy_threshold   = 2 # 2 pozytywne odpowiedzi = zdrowy
    unhealthy_threshold = 3 # 3 błędy = niezdrowy
  }#mozna by bylo dodac grace period aby dac czas na wstanięcie kontenerowi bo inaczej
  #jak dlugo wstaje  alb uzna go za niezdrowy i bedzie probowal go restartowac, co moze doprowadzic do sytuacji, ze kontener nigdy nie wstanie
}
# listener - słucha na porcie 80 i rozkłada ruch
resource "aws_lb_listener" "http" { # zasób listenera HTTP
  # ALB listener routes HTTP traffic to the auth-service target group.
  load_balancer_arn = aws_lb.auth.arn # przypisz do load balancera
  port              = 80 # nasłuchuj na porcie HTTP
  protocol          = "HTTP" # protokół komunikacji

  default_action { # konfiguracja akcji domyślnej
    type             = "forward" # prześlij ruch dalej
    target_group_arn = aws_lb_target_group.auth.arn # do naszej grupy
  }
}
# klaster ECS - kontener dla naszych zadań
resource "aws_ecs_cluster" "auth" { # zasób klastra ECS
  # ECS cluster hosting the auth-service tasks.
  name = "${var.project_name}-auth-cluster" # nazwa klastra
} 
# grupa logów CloudWatch dla ECS
resource "aws_cloudwatch_log_group" "auth" { # zasób grupy logów
  name              = "/ecs/${var.project_name}-auth-service" # ścieżka logów
  retention_in_days = 14 # przechowuj logi przez 14 dni
} 
# pobierz istniejącą rolę IAM z laboratorium
data "aws_iam_role" "lab_role" { # źródło danych istniejącej roli
  name = "LabRole"
}
# zmienne lokalne - konfiguracja środowiska dla aplikacji
locals { # definiuj zmienne lokalne, zmienne pomocniczne do konfiguracji kontenera
  base_environment = { # bazowe zmienne środowiskowe
    AWS_REGION            = var.aws_region # region AWS
    COGNITO_USER_POOL_ID  = aws_cognito_user_pool.auth.id # ID user pool
    COGNITO_CLIENT_ID     = aws_cognito_user_pool_client.auth.id # ID klienta
  }
# transformacja zmiennych do formatu ECS
  container_environment = [ # zmienne w formacie kontenera
    for key, value in merge(local.base_environment, var.extra_environment) : { # połącz zmienne
      name  = key
      value = value
    } # koniec transformacji
  ]
} 
# definicja zadania ECS - opis kontenera do uruchomienia
resource "aws_ecs_task_definition" "auth" { # zasób definicji zadania
  # ECS task definition for the auth-service container.
  family                   = "${var.project_name}-auth-service" # rodzina definicji
  requires_compatibilities = ["FARGATE"] # uruchamiaj na Fargate
  network_mode             = "awsvpc" # tryb sieci AWS VPC
  cpu                      = var.task_cpu # CPU dla zadania
  memory                   = var.task_memory # pamięć dla zadania
  execution_role_arn       = data.aws_iam_role.lab_role.arn # rola wykonawcza (arn - amz resource name)
  task_role_arn            = data.aws_iam_role.lab_role.arn # rola dla aplikacji
# definicja kontenera
  container_definitions = jsonencode([ # definicja kontenerów JSON
    { # konfiguracja kontenera
      name      = "auth-service" # nazwa kontenera
      image     = var.container_image # obraz dockera
      essential = true # kontener jest obligatoryjny
      portMappings = [ # mapowanie portów
        { # mapowanie portu
          containerPort = var.auth_container_port # port w kontenerze
          hostPort      = var.auth_container_port # port na hoście
          protocol      = "tcp" # protokół TCP
        } # koniec mapowania portu
      ]
      environment = local.container_environment # zmienne środowiskowe
      logConfiguration = { # konfiguracja logowania
        logDriver = "awslogs" # driver logów AWS
        options = { # opcje drivera
          awslogs-group         = aws_cloudwatch_log_group.auth.name # grupa logów
          awslogs-region        = var.aws_region # region AWS
          awslogs-stream-prefix = "ecs" # prefiks strumienia
        }
      }
    }
  ])
}
# usługa ECS - zarządza uruchamianiem zadań
resource "aws_ecs_service" "auth" { # zasób usługi ECS
  # ECS service keeps the desired number of auth-service tasks running.
  name            = "${var.project_name}-auth-service" # nazwa usługi
  cluster         = aws_ecs_cluster.auth.id # klaster do użycia
  task_definition = aws_ecs_task_definition.auth.arn # definicja zadania
  desired_count   = var.desired_count # liczba zadań do uruchomienia
  launch_type     = "FARGATE" # typ uruchomienia - Fargate
# konfiguracja sieci
  network_configuration { # ustawienia sieci
    subnets          = aws_subnet.public[*].id # podsieci dla zadań
    security_groups  = [aws_security_group.ecs.id]# kontrola ruchu
    assign_public_ip = true # przydzielaj publiczne IP
  }
# integracja z load balancerem
  load_balancer { # konfiguracja load balancera
    target_group_arn = aws_lb_target_group.auth.arn # grupa docelowa
    container_name   = "auth-service" # nazwa kontenera
    container_port   = var.auth_container_port
  }
# wymuszenie kolejności tworzenia zasobów
  depends_on = [aws_lb_listener.http]# najpierw tworzony load balancer
  # serwis musi wiedzieć do jakiego target group ma się podłączyć
  #przez listenera
} 
# cel autoscalingu - definiuje czym będziemy skalować
resource "aws_appautoscaling_target" "ecs" { # zasób celu autoscalingu
  # Autoscaling target config for ECS service desired count.
  max_capacity       = var.max_tasks # maksymalna liczba zadań
  min_capacity       = var.min_tasks # minimalna liczba zadań
  resource_id        = "service/${aws_ecs_cluster.auth.name}/${aws_ecs_service.auth.name}" # ID zasobu
  scalable_dimension = "ecs:service:DesiredCount" # skaluj liczbę zadań
  service_namespace  = "ecs" # namespace usługi ECS
} 
# polityka autoscalingu - skalowanie na podstawie CPU
resource "aws_appautoscaling_policy" "ecs_cpu" { # zasób polityki autoscalingu
  #automatyczne skalowanie usługi ECS. Jego zadaniem jest utrzymywanie
  # obciążenia procesora (CPU) na stałym, określonym poziomie poprzez
  #dodawanie lub usuwanie kontenerów.
  name               = "${var.project_name}-auth-cpu-scaling" # nazwa polityki
  policy_type        = "TargetTrackingScaling"# AWS sam oblicza skalowanie
  resource_id        = aws_appautoscaling_target.ecs.resource_id # ID zasobu
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension # okresla jaki zasób ma byc skalowany w naszym przypadku auth-service
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace # namespace
# konfiguracja polityki
  target_tracking_scaling_policy_configuration { # ustawienia śledzenia celu
    target_value = var.cpu_utilization_target # docelowe wykorzystanie CPU
# metryka do śledzenia
    predefined_metric_specification { # predefiniowana metryka
      predefined_metric_type = "ECSServiceAverageCPUUtilization" # średnie CPU
    }
# okresy czekania przed skalowaniem
    scale_in_cooldown  = 120 # czekaj 120 sekund przed zmniejszaniem
    scale_out_cooldown = 60 # czekaj 60 sekund przed zwiększaniem
  }
}