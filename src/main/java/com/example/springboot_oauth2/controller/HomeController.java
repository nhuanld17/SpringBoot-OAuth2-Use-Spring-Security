package com.example.springboot_oauth2.controller;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * Trang "connected" - minh họa metadata token sau khi đã ủy quyền.
     *
     * Nếu chưa ủy quyền, tham số @RegisteredOAuth2AuthorizedClient khiến Spring ném
     * ClientAuthorizationRequiredException -> tự động redirect sang Google consent
     * rồi quay lại chính /connected.
     */
    @GetMapping("/connected")
    public String connected(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Model model) {
        OAuth2AccessToken accessToken = client.getAccessToken();

        model.addAttribute("hasAccessToken", accessToken != null);
        model.addAttribute("hasRefreshToken", client.getRefreshToken() != null);
        model.addAttribute("expiresAt", accessToken.getExpiresAt()); // Instant
        model.addAttribute("scopes", accessToken.getScopes());       // Set<String>
        model.addAttribute("tokenType", accessToken.getTokenType().getValue());

        return "connected";
    }
}
