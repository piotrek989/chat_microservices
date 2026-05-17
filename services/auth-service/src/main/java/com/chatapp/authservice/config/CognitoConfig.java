package com.chatapp.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cognito")
public class CognitoConfig {

    private String userPoolId;
    private String clientId;
}
