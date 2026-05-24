#!/bin/bash

# 1. S3 - Tworzenie bucketu na pliki
awslocal s3 mb s3://chatapp-file-bucket

# 2. DynamoDB - Tworzenie tabeli na metadane plików
awslocal dynamodb create-table \
    --table-name chatapp-file-metadata \
    --attribute-definitions AttributeName=fileId,AttributeType=S \
    --key-schema AttributeName=fileId,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

# 3. SNS - Tworzenie tematu powiadomień
# Nazwa musi być taka sama jak w Twoim application.yml / docker-compose
awslocal sns create-topic --name chatapp-notifications

# 4. SNS Subscription - Łączenie SNS z Twoim Notification-Service
# WAŻNE: Używamy nazwy serwisu z docker-compose ('notification-service') jako hosta
awslocal sns subscribe \
    --topic-arn arn:aws:sns:us-east-1:000000000000:chatapp-notifications \
    --protocol http \
    --notification-endpoint http://notification-service:8083/notifications/sns