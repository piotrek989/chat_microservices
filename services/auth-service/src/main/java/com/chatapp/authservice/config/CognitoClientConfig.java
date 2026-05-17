package com.chatapp.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoClientConfig {

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient(AwsConfig awsConfig) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(awsConfig.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
