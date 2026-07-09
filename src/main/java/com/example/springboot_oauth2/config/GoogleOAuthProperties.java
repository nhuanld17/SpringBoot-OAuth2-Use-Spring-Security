package com.example.springboot_oauth2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anh xa cac property "google.oauth.*" trong application.properties thanh 1 object Java.
 * Dung record cho gon - Spring Boot tu binding theo ten (kebab-case -> camelCase).
 */
@ConfigurationProperties(prefix = "google.oauth")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String scope,
        String authorizationUri,
        String tokenUri
) {
}
