package com.chatapp.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {
    @Bean
    public Region awsRegion(@Value("${aws.region}") String region) {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(Region region,
                             @Value("${aws.access-key}") String accessKey,
                             @Value("${aws.secret-key}") String secretKey,
                             @Value("${aws.s3-endpoint:}") String s3Endpoint) {
        S3ClientBuilder builder = S3Client.builder()
            .region(region)
            .credentialsProvider(resolveCredentials(accessKey, secretKey))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());
        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient(Region region,
                                         @Value("${aws.access-key}") String accessKey,
                                         @Value("${aws.secret-key}") String secretKey,
                                         @Value("${aws.dynamodb-endpoint:}") String dynamodbEndpoint) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
            .region(region)
            .credentialsProvider(resolveCredentials(accessKey, secretKey));
        if (dynamodbEndpoint != null && !dynamodbEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(dynamodbEndpoint));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(Region region,
                                @Value("${aws.access-key}") String accessKey,
                                @Value("${aws.secret-key}") String secretKey,
                                @Value("${aws.s3-endpoint:}") String s3Endpoint) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(region)
            .credentialsProvider(resolveCredentials(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
                        
        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint));
        }
        return builder.build();
    }

    @Bean
    public SnsClient snsClient(Region region,
                               @Value("${aws.access-key}") String accessKey,
                               @Value("${aws.secret-key}") String secretKey,
                               @Value("${aws.sns-endpoint:}") String snsEndpoint) {
        SnsClientBuilder builder = SnsClient.builder()
                .region(region)
                .credentialsProvider(resolveCredentials(accessKey, secretKey));
        if (snsEndpoint != null && !snsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpoint));
        }
        return builder.build();
    }

    private AwsCredentialsProvider resolveCredentials(String accessKey, String secretKey) {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}