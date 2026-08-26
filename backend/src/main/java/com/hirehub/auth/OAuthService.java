package com.hirehub.auth;

import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.OAuthCallbackRequest;
import com.hirehub.auth.entity.OAuthProvider;
import com.hirehub.auth.entity.RefreshToken;
import com.hirehub.auth.repository.OAuthProviderRepository;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OAuthService {

    private final OAuthProviderRepository oAuthProviderRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.github.client-secret:}")
    private String githubClientSecret;

    public OAuthService(
            OAuthProviderRepository oAuthProviderRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            StudentRepository studentRepository,
            RecruiterRepository recruiterRepository,
            JwtService jwtService) {
        this.oAuthProviderRepository = oAuthProviderRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.studentRepository = studentRepository;
        this.recruiterRepository = recruiterRepository;
        this.jwtService = jwtService;
        this.restTemplate = new RestTemplate();
    }

    // ── Google OAuth ──

    @Transactional
    public AuthResponse handleGoogleCallback(OAuthCallbackRequest request) {
        String code = request.getCode();

        // Exchange authorization code for tokens
        String tokenUrl = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "code=" + code
                + "&client_id=" + googleClientId
                + "&client_secret=" + googleClientSecret
                + "&redirect_uri=" + (request.getRedirectUri() != null ? request.getRedirectUri() : "")
                + "&grant_type=authorization_code";

        HttpEntity<String> tokenRequest = new HttpEntity<>(body, headers);
        ResponseEntity<Map> tokenResponse;
        try {
            tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
        } catch (Exception e) {
            log.error("Failed to exchange Google authorization code: {}", e.getMessage());
            throw new IllegalStateException("Failed to authenticate with Google");
        }

        if (tokenResponse.getBody() == null || !tokenResponse.getBody().containsKey("access_token")) {
            throw new IllegalStateException("Invalid Google authorization response");
        }

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        // Fetch user info from Google
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userInfo;
        try {
            userInfo = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    userRequest,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to fetch Google user info: {}", e.getMessage());
            throw new IllegalStateException("Failed to fetch user information from Google");
        }

        if (userInfo.getBody() == null) {
            throw new IllegalStateException("No user information returned from Google");
        }

        Map<String, Object> googleUser = userInfo.getBody();
        String providerUserId = (String) googleUser.get("sub");
        String email = (String) googleUser.get("email");
        String name = (String) googleUser.get("name");
        String picture = (String) googleUser.get("picture");

        return processOAuthUser("GOOGLE", providerUserId, email, name, picture);
    }

    // ── GitHub OAuth ──

    @Transactional
    public AuthResponse handleGithubCallback(OAuthCallbackRequest request) {
        String code = request.getCode();

        // Exchange authorization code for access token
        String tokenUrl = "https://github.com/login/oauth/access_token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        String body = "code=" + code
                + "&client_id=" + githubClientId
                + "&client_secret=" + githubClientSecret;

        HttpEntity<String> tokenRequest = new HttpEntity<>(body, headers);
        ResponseEntity<Map> tokenResponse;
        try {
            tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
        } catch (Exception e) {
            log.error("Failed to exchange GitHub authorization code: {}", e.getMessage());
            throw new IllegalStateException("Failed to authenticate with GitHub");
        }

        if (tokenResponse.getBody() == null || !tokenResponse.getBody().containsKey("access_token")) {
            throw new IllegalStateException("Invalid GitHub authorization response");
        }

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        // Fetch user info from GitHub
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        userHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userInfo;
        try {
            userInfo = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    userRequest,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info: {}", e.getMessage());
            throw new IllegalStateException("Failed to fetch user information from GitHub");
        }

        if (userInfo.getBody() == null) {
            throw new IllegalStateException("No user information returned from GitHub");
        }

        Map<String, Object> githubUser = userInfo.getBody();
        String providerUserId = String.valueOf(githubUser.get("id"));
        String name = (String) githubUser.get("name");
        String login = (String) githubUser.get("login");
        String picture = (String) githubUser.get("avatar_url");

        // GitHub may not provide email via primary API; fetch from emails endpoint
        String email = (String) githubUser.get("email");
        if (email == null || email.isBlank()) {
            email = fetchGithubEmail(accessToken);
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("GitHub account does not have a public email. Please add one in GitHub settings.");
        }

        return processOAuthUser("GITHUB", providerUserId, email, name != null ? name : login, picture);
    }

    private String fetchGithubEmail(String accessToken) {
        try {
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            userHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<java.util.List> emailResponse = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    userRequest,
                    java.util.List.class
            );

            if (emailResponse.getBody() != null) {
                for (Object obj : emailResponse.getBody()) {
                    Map<String, Object> emailEntry = (Map<String, Object>) obj;
                    if (Boolean.TRUE.equals(emailEntry.get("primary")) && Boolean.TRUE.equals(emailEntry.get("verified"))) {
                        return (String) emailEntry.get("email");
                    }
                }
                // Fallback to first verified email
                for (Object obj : emailResponse.getBody()) {
                    Map<String, Object> emailEntry = (Map<String, Object>) obj;
                    if (Boolean.TRUE.equals(emailEntry.get("verified"))) {
                        return (String) emailEntry.get("email");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub emails: {}", e.getMessage());
        }
        return null;
    }

    // ── Common OAuth processing ──

    private AuthResponse processOAuthUser(String provider, String providerUserId, String email, String name, String image) {
        email = email.toLowerCase().trim();

        // 1. Check if this OAuth account is already linked
        Optional<OAuthProvider> existingProvider = oAuthProviderRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingProvider.isPresent()) {
            User user = existingProvider.get().getUser();
            log.info("OAuth login: {} user {} linked to {}", provider, email, user.getEmail());
            return buildAuthResponse(user);
        }

        // 2. Check if a HireHub account exists with this email
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Link OAuth to existing account
            User user = existingUser.get();
            OAuthProvider oauthProvider = OAuthProvider.builder()
                    .user(user)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .email(email)
                    .name(name)
                    .image(image)
                    .build();
            oAuthProviderRepository.save(oauthProvider);

            // Update user image if not set
            if (user.getImage() == null && image != null) {
                user.setImage(image);
                userRepository.save(user);
            }

            log.info("OAuth linked: {} account linked to existing user {}", provider, email);
            return buildAuthResponse(user);
        }

        // 3. Create new user (role=null → pending role selection)
        User user = User.builder()
                .email(email)
                .passwordHash(null) // No password for OAuth users
                .name(name)
                .image(image)
                .role(null) // Role not yet selected
                .isAnonymous(false)
                .emailVerified(true) // OAuth emails are verified
                .build();
        user = userRepository.save(user);

        // Create OAuth link
        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .name(name)
                .image(image)
                .build();
        oAuthProviderRepository.save(oauthProvider);

        log.info("OAuth new user created: {} via {}", email, provider);
        return buildAuthResponse(user);
    }

    // ── Role selection for OAuth users ──

    @Transactional
    public AuthResponse selectRole(String email, Role role) {
        if (role == Role.ADMIN) {
            throw new ForbiddenException("Cannot register as admin through OAuth");
        }
        if (role != Role.STUDENT && role != Role.RECRUITER) {
            throw new ForbiddenException("Invalid role. Must be STUDENT or RECRUITER");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.hirehub.common.exception.UnauthorizedException("Account not found"));

        if (user.getRole() != null) {
            throw new com.hirehub.common.exception.IllegalStateException("Role already set");
        }

        user.setRole(role);
        user.setRoleSelectedAt(OffsetDateTime.now());
        userRepository.save(user);

        // Create role-specific profile
        if (role == Role.STUDENT) {
            Student student = Student.builder()
                    .user(user)
                    .profileComplete(false)
                    .build();
            studentRepository.save(student);
        } else if (role == Role.RECRUITER) {
            Recruiter recruiter = Recruiter.builder()
                    .user(user)
                    .build();
            recruiterRepository.save(recruiter);
        }

        log.info("OAuth user {} selected role {}", email, role);
        return buildAuthResponse(user);
    }

    // ── Helpers ──

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                toUserDetails(user),
                Map.of("role", user.getRole() != null ? user.getRole().name() : "PENDING")
        );
        String refreshToken = jwtService.generateRefreshToken(toUserDetails(user));

        RefreshToken storedRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(refreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(storedRefreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(AuthService.toUserResponse(user))
                .build();
    }

    private org.springframework.security.core.userdetails.UserDetails toUserDetails(User user) {
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "";
        String role = user.getRole() != null ? user.getRole().name() : "STUDENT";
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + role
                ))
        );
    }
}
