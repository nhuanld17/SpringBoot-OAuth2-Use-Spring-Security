package com.example.springboot_oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PHASE 2: Giao toàn bộ luồng OAuth2 (authorized, callback, PKCE, refresh, lưu token)
 * cho Spring Security OAuth2 Client.
 *
 * Ta dùng oauth2Client() THUẦN (không dùng oauth2Login()) vì đây là
 * DELEGATED AUTHORIZATION - app được uỷ quyền gọi Google Calendar API thay cho người dùng,
 * KHÔNG phải đăng nhập/OIDC. Vi vậy không có principal/login; authorized client được lưu theo
 * theo HttpSession.
 *
 * Việc xin ủy quyền sẽ TỰ ĐỘNG kích hoạt khi controller cần accesstoken
 * (qua @RegisteredOAuth2AuthorizationClient) - Spring ném ClientAuthorizationRequiredException
 * -> redirect sang Google -> callback -> quay lại request gốc
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                // Không bắt login: mỗi request đề được phép; ủy quyền chỉ kích hoạt khi cần token
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Client(oauth2 -> oauth2
                        .authorizationCodeGrant(codeGrant -> codeGrant
                                .authorizationRequestResolver(
                                        pkceAuthorizationRequestResolver(clientRegistrationRepository))
                        )
                );
        return http.build();
    }

    /**
     * Resolver gắn thêm vào authorization request:
     * - PKCE (code_challenge + method=S256) qua OAuth2AuthorizationRequestCustomizers.withPkce()
     * - access_type=offline + prompt=consent để Google trả về REFRESH token.
     */
    private OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(builder -> {
            OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
            builder.additionalParameters(params -> {
                params.put("access_type", "offline");
                params.put("prompt", "consent");
            });
        });
        return resolver;
    }

    /**
     * Manager với provider authorizationCode + refreshToken.
     * Nhờ refreshToken() mà khi access token hết hạn, lần kế @RegisteredOAuth2AuthorizedClient
     * sẽ TỰ ĐỘNG dùng refresh_token đổi access token mới (trong suốt, không bắt consent lại).
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider provider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }
}
