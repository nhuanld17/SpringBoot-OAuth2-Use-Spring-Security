package com.example.springboot_oauth2.response;

/**
 * Class chứa response google
 */
public record TokenResponse (
        String access_token,
        Integer expires_in,
        String refresh_token,
        String scope,
        String token_type,
        String id_token
){
}
