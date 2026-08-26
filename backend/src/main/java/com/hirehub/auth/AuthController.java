package com.hirehub.auth;

import com.hirehub.auth.dto.*;
import com.hirehub.common.ApiResponse;
import com.hirehub.common.RateLimiter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final String REFRESH_COOKIE = "refresh_token";

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response,
            HttpServletRequest requestCtx) {
        rateLimiter.checkAuthRateLimit("register:" + getClientIp(requestCtx));
        AuthResponse authResponse = authService.register(request);
        addRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(stripRefreshToken(authResponse)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response,
            HttpServletRequest requestCtx) {
        rateLimiter.checkAuthRateLimit("login:" + getClientIp(requestCtx));
        AuthResponse authResponse = authService.login(request);
        addRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(stripRefreshToken(authResponse)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) RefreshTokenRequest body) {
        // Read refresh token from HttpOnly cookie first, fall back to body for backward compat
        String refreshToken = extractRefreshFromCookie(request);
        if (refreshToken == null && body != null && body.getRefreshToken() != null) {
            refreshToken = body.getRefreshToken();
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No refresh token provided"));
        }

        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(refreshToken);
        AuthResponse authResponse = authService.refresh(refreshReq);
        addRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(stripRefreshToken(authResponse)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            authService.logout(auth.getName());
        }
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserResponse user = authService.getMe(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    // ── Cookie helpers ──

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction());
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction());
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String extractRefreshFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    private AuthResponse stripRefreshToken(AuthResponse response) {
        return AuthResponse.builder()
                .accessToken(response.getAccessToken())
                .tokenType(response.getTokenType())
                .expiresIn(response.getExpiresIn())
                .user(response.getUser())
                // Refresh token sent via HttpOnly cookie, not in response body
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
