package com.example.springboot_oauth2.controller;

import com.example.springboot_oauth2.config.GoogleOAuthProperties;
import com.example.springboot_oauth2.response.TokenResponse;
import com.example.springboot_oauth2.service.GoogleOAuthService;
import com.example.springboot_oauth2.service.TokenService;
import com.example.springboot_oauth2.utils.PkceUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Controller
public class AuthController {

    private final GoogleOAuthProperties props;
    private final GoogleOAuthService oauthService;
    private final TokenService tokenService;

    // Tên key lưu trong session (dừng lại ở bước callback)
    public static final String SESSION_STATE = "oauth_state";
    public static final String SESSION_CODE_VERIFIER = "oauth_code_verifier";
    public static final String SESSION_TOKEN = "oauth_token";
    public static final String SESSION_EXPIRES_AT = "oauth_expires_at";

    public AuthController(GoogleOAuthProperties props, GoogleOAuthService oauthService, TokenService tokenService) {
        this.props = props;
        this.oauthService = oauthService;
        this.tokenService = tokenService;
    }

    /*
     * Browser
     *    |
     *    | GET /oauth2/authorize
     *    |
     * Spring Boot
     *    |
     *    | Sinh state (chống CSRF)
     *    | Sinh PKCE (code_verifier & code_challenge)
     *    | Lưu state + code_verifier vào HttpSession
     *    | Build URL Authorization của Google
     *    |
     *    | HTTP 302 Redirect
     *    |
     *    v
     * https://accounts.google.com/o/oauth2/v2/auth?...
     *    |
     *    | Người dùng đăng nhập Google và cấp quyền
     *    |
     * Google redirect về
     *    |
     *    v
     * GET /oauth2/callback?code=...&state=...
     */
    @GetMapping("/oauth2/authorize")
    public String authorize(HttpSession session) {
        // 1. Sinh state (chống CSRF) và PKCE (bảo vệ authorization code)
        String state = PkceUtil.generateState();
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);

        // 2. Lưu state + code_verifier vào session của người dùng này
        // Call back sẽ so lại state, và dùng code_verifier để đổi token
        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_CODE_VERIFIER, codeVerifier);

        // 3) Build authorization request URL gui toi Google
        String authorizeUrl = UriComponentsBuilder
                .fromUriString(props.authorizationUri())
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("response_type", "code")          // xin authorization CODE
                .queryParam("scope", props.scope())
                .queryParam("state", state)                   // chong CSRF
                .queryParam("code_challenge", codeChallenge)  // PKCE
                .queryParam("code_challenge_method", "S256")
                .queryParam("access_type", "offline")         // xin ca REFRESH token
                .queryParam("prompt", "consent")              // luon hien consent (de
        //chac chan co refresh token)
                .build()
                .encode()   // encode dung cach (scope co dau ":" va "/")
                .toUriString();

        // 4) Redirect trinh duyet sang Google
        return "redirect:" + authorizeUrl;
    }

    @GetMapping("/oauth2/callback")
    public String callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session,
            Model model
    ) {
        // 0. Nếu người dùng bấm "cancel" -> Google trả về ?error=access_denied
        if (error != null) {
            model.addAttribute("error", "Google trả về lỗi: " + error);
            return "error-page";
        }

        // 1. Kiểm tra state (chống CSRF):
        // state trong callback phải trùng với state đã lưu trong session
        String savedState = (String) session.getAttribute(SESSION_STATE);
        if (savedState == null || !savedState.equals(state)) {
            model.addAttribute("error", "State không khớp -> Nghi ngờ CSRF. Từ chối");
            return "error-page";
        }

        // 2. Lấy lại code verifier đã lưu để đổi token (PKCE)
        String codeVerifier = (String) session.getAttribute(SESSION_CODE_VERIFIER);

        System.out.println(">>> state param    = " + state);
        System.out.println(">>> state session  = " + savedState);
        System.out.println(">>> codeVerifier    = " + codeVerifier);

        // 3. Đổi code lấy token (gọi server-to-server tới google)
        TokenResponse token;

        try {
            token = oauthService.exchangeCodeForToken(code, codeVerifier);
        } catch (RestClientResponseException ex) {
            model.addAttribute("error",
                    "Đổi code lấy token thất bại (" + ex.getStatusCode().value() + "): " + ex.getResponseBodyAsString());
            return "error-page";
        }

        // 4. Lưu token vào session để các request sau dùng gọi Calendar API
        session.setAttribute(SESSION_TOKEN, token);

        // Tính thời điểm accesstoken hết hạn = bây giờ + expires_in (second)
        Instant expiresAt = Instant.now().plusSeconds(token.expires_in());
        session.setAttribute(SESSION_EXPIRES_AT, expiresAt);

        // Dọn dẹp state + verifier (dùng 1 lần rồi bỏ)
        session.removeAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_CODE_VERIFIER);

        // 5. Hiện thông tin ra FE
        model.addAttribute("hasAccessToken", token.access_token()
                != null);
        model.addAttribute("hasRefreshToken", token.refresh_token()  != null);
        model.addAttribute("expiresIn", token.expires_in());
        model.addAttribute("scope", token.scope());
        model.addAttribute("tokenType", token.token_type());

        return "connected";
    }

    @GetMapping("/oauth2/refresh")
    public String forceRefresh(HttpSession session, Model model) {
        TokenResponse current = (TokenResponse)
                session.getAttribute(SESSION_TOKEN);
        if (current == null || current.refresh_token() == null) {
            model.addAttribute("error", "Chưa có refresh token - hãy connect lại");
            return "error-page";
        }

        String oldAccess = current.access_token();
        TokenResponse refreshed;

        try {
            refreshed = tokenService.refresh(session, current);  // ép refresh access token ngay
        } catch (RestClientResponseException ex) {
            session.removeAttribute(SESSION_TOKEN);
            session.removeAttribute(SESSION_EXPIRES_AT);

            model.addAttribute("error",
                    "Refresh token không còn hợp lệ (có thể đã bị thu hồi). Hãy connect lại.");

            return "error-page";
        }

        // So sanh 20 ky tu dau de thay token da DOI
        model.addAttribute("oldPrefix", oldAccess.substring(0, 20) +
                "...");
        model.addAttribute("newPrefix",
                refreshed.access_token().substring(0, 20) + "...");
        model.addAttribute("newExpiresIn", refreshed.expires_in());

        return "refreshed";
    }
}
