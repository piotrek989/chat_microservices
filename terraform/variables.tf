variable "project_name" {
  description = "Prefix used for naming AWS resources."
  type        = string
  default     = "chatapp"
}

variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

# Główny zakres dla VPC (pojedynczy string)
variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

# Zakresy dla podsieci (lista stringów)
variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.1.0/24", "10.0.2.0/24"]#dwie podsieci w dwóch access zones
  #gdy jedna padnie to druga nadal będzie działać, co zwiększa dostępność i odporność infrastruktury.
}

variable "container_image" {
  description = "Docker image for the auth-service (e.g., account.dkr.ecr.us-east-1.amazonaws.com/auth-service:latest)."
  type        = string
}

variable "container_port" {
  description = "Container port exposed by auth-service."
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "Fargate task CPU units."
  type        = string
  default     = "512"
}

variable "task_memory" {
  description = "Fargate task memory in MiB."
  type        = string
  default     = "1024"
}

variable "desired_count" {
  description = "Initial number of ECS tasks."
  type        = number
  default     = 2
}

variable "min_tasks" {
  description = "Minimum number of tasks for autoscaling."
  type        = number
  default     = 2
}

variable "max_tasks" {
  description = "Maximum number of tasks for autoscaling."
  type        = number
  default     = 3
}

variable "cpu_utilization_target" {
  description = "Target CPU utilization percentage for autoscaling."
  type        = number
  default     = 70
}

variable "extra_environment" {
  description = "Additional environment variables passed to the container."
  type        = map(string)
  default     = {}
}
