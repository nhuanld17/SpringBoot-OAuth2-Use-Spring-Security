package com.example.springboot_oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /**
     * RestClient la HTTP client hien dai cua Spring (thay cho RestTemplate).
     * Ta dung no de goi token endpoint + Google Calendar API.
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
