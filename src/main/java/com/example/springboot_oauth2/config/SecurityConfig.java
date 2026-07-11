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
 * PHASE 2: Giao toàn bộ luồng OAuth2 (authorize, callback, PKCE, refresh, lưu token)
 * cho Spring Security OAuth2 Client.
 *
 * Ta dùng oauth2Client() THUẦN (không dùng oauth2Login()) vì đây là
 * DELEGATED AUTHORIZATION - app được ủy quyền gọi Google Calendar API thay cho người dùng,
 * KHÔNG phải đăng nhập/OIDC. Vì vậy không có principal/login; authorized client được lưu
 * theo HttpSession.
 *
 * Việc xin ủy quyền sẽ TỰ ĐỘNG kích hoạt khi controller cần access token
 * (qua @RegisteredOAuth2AuthorizedClient) - Spring ném ClientAuthorizationRequiredException
 * -> redirect sang Google -> callback -> quay lại request gốc.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                // Không bắt login: mọi request đều được phép; ủy quyền chỉ kích hoạt khi cần token
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
        // resolver này lắng nghe ở base URI /oauth2/authorization.
        // Tức là URL khởi động flow sẽ là /oauth2/authorization/google (google là registrationId).
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(builder -> {
            // Bật PKCE, Spring tự động tạo code_challenge, code_challenge_method=S256, code_verifer
            OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
            builder.additionalParameters(params -> {
                params.put("access_type", "offline"); // offline: xin thêm refresh token
                params.put("prompt", "consent");
            });
        });
        return resolver;
    }

    /**
     * OAuth2AuthorizedClientManager là thành phần điều phối trung tâm cho việc "lấy được một
     * authorized client". Nó chịu trách nhiệm quyết định: khi controller cần token thì nên cấp
     * mới (qua Authorization Code), refresh token cũ, hay trả về token đang có — bằng cách ủy
     * quyền cho các OAuth2AuthorizedClientProvider.
     *
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
