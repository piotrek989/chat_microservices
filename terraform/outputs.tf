# output "cognito_user_pool_id" {
#   description = "Cognito User Pool ID."
#   value       = var.cognito_user_pool_id
# }

# output "cognito_user_pool_client_id" {
#   description = "Cognito User Pool Client ID."
#   value       = var.cognito_client_id
# }

output "aws_region" {
  description = "AWS region used for deployment."
  value       = var.aws_region
}

output "alb_url" {
  description = "ALB URL for the auth-service."
  value       = aws_lb.auth.dns_name
}

output "chat_alb_url" {
  description = "ALB URL for the chat-service."
  value       = aws_lb.chat.dns_name
}
