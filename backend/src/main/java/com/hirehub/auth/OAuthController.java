package com.hirehub.auth;

import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.OAuthCallbackRequest;
import com.hirehub.common.ApiResponse;
import com.hirehub.common.RateLimiter;
import com.hirehub.common.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final RateLimiter rateLimiter;

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleCallback(
            @Valid @RequestBody OAuthCallbackRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAuthRateLimit("oauth-google:" + getClientIp(httpRequest));
        AuthResponse response = oAuthService.handleGoogleCallback(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/github")
    public ResponseEntity<ApiResponse<AuthResponse>> githubCallback(
            @Valid @RequestBody OAuthCallbackRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAuthRateLimit("oauth-github:" + getClientIp(httpRequest));
        AuthResponse response = oAuthService.handleGithubCallback(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    /**
     * Set role for OAuth users who don't have a role yet.
     * Must be authenticated.
     */
    @PostMapping("/role")
    public ResponseEntity<ApiResponse<AuthResponse>> selectRole(
            @RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        String roleStr = request.get("role");
        if (roleStr == null || roleStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role is required"));
        }

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid role"));
        }

        AuthResponse response = oAuthService.selectRole(auth.getName(), role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
