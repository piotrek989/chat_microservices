package com.chatapp.notificationservice.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

@Configuration
public class SnsConfig {
    @Bean
    public SnsClient snsClient(
        @Value("${aws.region}") String region,
        @Value("${aws.sns-endpoint:}") String snsEndpoint
    ) {
        SnsClientBuilder builder = SnsClient.builder().region(Region.of(region));
        if (snsEndpoint != null && !snsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpoint));
        }
        return builder.build();
    }
}
