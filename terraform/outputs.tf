output "cognito_user_pool_id" {
  description = "Cognito User Pool ID."
  value       = aws_cognito_user_pool.auth.id
}

output "cognito_user_pool_client_id" {
  description = "Cognito User Pool Client ID."
  value       = aws_cognito_user_pool_client.auth.id
}

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

output "file_alb_url" {
  description = "ALB URL for the file-service."
  value       = aws_lb.file.dns_name
}

output "file_bucket_name" {
  description = "S3 bucket name for file uploads."
  value       = aws_s3_bucket.file.bucket
}

output "file_table_name" {
  description = "DynamoDB table name for file metadata."
  value       = aws_dynamodb_table.file.name
}

output "notification_alb_url" {
  description = "ALB URL for the notification-service."
  value       = aws_lb.notification.dns_name
}

output "notification_sns_topic_arn" {
  description = "SNS topic ARN for notifications."
  value       = aws_sns_topic.notifications.arn
}

output "frontend_bucket_name" {
  description = "S3 bucket name for the frontend."
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_website_url" {
  description = "S3 website endpoint for the frontend."
  value       = aws_s3_bucket_website_configuration.frontend.website_endpoint
}

output "frontend_vite_env" {
  description = "Vite env values for frontend build."
  value = {
    VITE_AUTH_API            = "http://${aws_lb.auth.dns_name}"
    VITE_CHAT_API            = "http://${aws_lb.chat.dns_name}"
    VITE_FILE_API            = "http://${aws_lb.file.dns_name}"
    VITE_NOTIFICATION_API    = "http://${aws_lb.notification.dns_name}"
    VITE_AUTH_CONFIRM_ENDPOINT = ""
  }
}
