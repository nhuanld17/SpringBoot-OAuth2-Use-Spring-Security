package com.example.springboot_oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PHASE 1: Vi ta TU dung OAuth2 flow bang tay, ta KHONG dung tinh nang dang nhap
 * cua Spring Security. Nhung spring-boot-starter-oauth2-client keo theo Spring Security,
 * ma mac dinh Spring Security khoa TAT CA request -> bat dang nhap.
 *
 * Nen o day ta mo het (permitAll) de flow thu cong hoat dong.
 * Sang Phase 2 ta se thay config nay bang cau hinh OAuth2 that su cua Spring Security.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Demo: tat CSRF cho don gian (flow cua ta chi dung GET redirect).
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
