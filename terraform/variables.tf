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

variable "auth_container_port" {
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

variable "chat_container_image" {
  description = "Docker image for the chat-service (e.g., account.dkr.ecr.us-east-1.amazonaws.com/chat-service:latest)."
  type        = string
}

variable "chat_container_port" {
  description = "Container port exposed by chat-service."
  type        = number
  default     = 8081
}

variable "chat_db_name" {
  description = "Database name for chat-service."
  type        = string
  default     = "chatdb"
}

variable "chat_db_username" {
  description = "Database username for chat-service."
  type        = string
  default     = "postgres"
}

variable "chat_db_password" {
  description = "Database password for chat-service."
  type        = string
  sensitive   = true
  default     = "postgres"
}

variable "chat_jwt_issuer_uri" {
  description = "JWT issuer URI for chat-service."
  type        = string
  default     = "https://cognito-idp.us-east-1.amazonaws.com/your-user-pool-id"
}

variable "file_container_image" {
  description = "Docker image for the file-service (e.g., account.dkr.ecr.us-east-1.amazonaws.com/file-service:latest)."
  type        = string
}

variable "file_container_port" {
  description = "Container port exposed by file-service."
  type        = number
  default     = 8082
}

variable "file_bucket_name" {
  description = "S3 bucket name used by file-service."
  type        = string
  default     = "chatapp-file-bucket"
}

variable "file_table_name" {
  description = "DynamoDB table name used by file-service."
  type        = string
  default     = "chatapp-file-metadata"
}

# variable "file_jwt_issuer_uri" {
#   description = "JWT issuer URI for file-service."
#   type        = string
#   default     = "https://cognito-idp.us-east-1.amazonaws.com/your-user-pool-id"
# }

variable "notification_container_image" {
  description = "Docker image for the notification-service (e.g., account.dkr.ecr.us-east-1.amazonaws.com/notification-service:latest)."
  type        = string
}

variable "notification_container_port" {
  description = "Container port exposed by notification-service."
  type        = number
  default     = 8083
}

variable "notification_db_name" {
  description = "Database name for notification-service."
  type        = string
  default     = "notificationdb"
}

variable "notification_db_username" {
  description = "Database username for notification-service."
  type        = string
  default     = "postgres"
}

variable "notification_db_password" {
  description = "Database password for notification-service."
  type        = string
  sensitive   = true
  default     = "postgres"
}

# variable "notification_jwt_issuer_uri" {
#   description = "JWT issuer URI for notification-service."
#   type        = string
#   default     = "https://cognito-idp.us-east-1.amazonaws.com/your-user-pool-id"
# }
