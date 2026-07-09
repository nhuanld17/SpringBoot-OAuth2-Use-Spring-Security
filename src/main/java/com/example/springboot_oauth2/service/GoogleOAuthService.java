package com.example.springboot_oauth2.service;

import com.example.springboot_oauth2.config.GoogleOAuthProperties;
import com.example.springboot_oauth2.response.TokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class GoogleOAuthService {

    private final RestClient restClient;
    private final GoogleOAuthProperties props;

    public GoogleOAuthService(RestClient restClient, GoogleOAuthProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public TokenResponse exchangeCodeForToken(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code"); // Dạng exchange code
        form.add("code", code); // Authorization Code mà Google trả về
        form.add("redirect_uri", props.redirectUri());
        form.add("code_verifier", codeVerifier);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        return restClient.post()
                .uri(props.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    /**
     * Dùng refreshToken để xin accesstoken mới (không cần user consent lại)
     * Note: response thường không kèm refresh_token mới
     */
    public TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "refresh_token"); // <== Khác authorization code
        form.add("refresh_token", refreshToken);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        return restClient.post()
                .uri(props.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }
}
