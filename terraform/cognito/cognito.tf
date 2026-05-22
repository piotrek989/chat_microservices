variable "project_name" {
  description = "Prefix used for naming AWS resources."
  type        = string
  default     = "chatapp"
}

resource "aws_cognito_user_pool" "auth" {
  # Cognito user pool for email-based authentication.
  name = "${var.project_name}-auth-user-pool"

  auto_verified_attributes  = ["email"]

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

  generate_secret = false

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]
}