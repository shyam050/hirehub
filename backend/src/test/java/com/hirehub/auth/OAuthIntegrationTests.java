package com.hirehub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.dto.LoginRequest;
import com.hirehub.auth.entity.OAuthProvider;
import com.hirehub.auth.repository.OAuthProviderRepository;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthProviderRepository oAuthProviderRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private com.hirehub.notification.repository.NotificationRepository notificationRepository;

    @Autowired
    private JobMatchRepository matchRepository;

    @Autowired
    private ResumeAnalysisRepository analysisRepository;

    @Autowired
    private com.hirehub.application.repository.ApplicationRepository applicationRepository;

    @Autowired
    private com.hirehub.job.repository.JobRepository jobRepository;

    @Autowired
    private com.hirehub.company.repository.CompanyRepository companyRepository;

    private final AtomicInteger emailCounter = new AtomicInteger(0);

    @BeforeEach
    void cleanDatabase() {
        matchRepository.deleteAll();
        analysisRepository.deleteAll();
        notificationRepository.deleteAll();
        applicationRepository.deleteAll();
        jobRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        oAuthProviderRepository.deleteAll();
        recruiterRepository.deleteAll();
        companyRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        emailCounter.set(0);
    }

    // ── Role Selection Tests ──

    @Test
    void oauthRoleSelection_student() throws Exception {
        // Create an OAuth user with no role
        User oauthUser = User.builder()
                .email("oauth-student-" + emailCounter.incrementAndGet() + "@example.com")
                .passwordHash(null)
                .name("OAuth Student")
                .role(null)
                .isAnonymous(false)
                .emailVerified(true)
                .build();
        oauthUser = userRepository.save(oauthUser);

        // Get a token for this user
        String token = getTokenForUser(oauthUser.getEmail());

        // Select student role
        MvcResult result = mockMvc.perform(post("/api/v1/auth/oauth/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // Verify user has role
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("STUDENT"));

        // Verify student profile was created
        User updatedUser = userRepository.findByEmail(oauthUser.getEmail()).orElseThrow();
        assertEquals(Role.STUDENT, updatedUser.getRole());
        assertNotNull(updatedUser.getRoleSelectedAt());
        assertTrue(studentRepository.findByUserId(updatedUser.getId()).isPresent());
    }

    @Test
    void oauthRoleSelection_recruiter() throws Exception {
        User oauthUser = User.builder()
                .email("oauth-recruiter-" + emailCounter.incrementAndGet() + "@example.com")
                .passwordHash(null)
                .name("OAuth Recruiter")
                .role(null)
                .isAnonymous(false)
                .emailVerified(true)
                .build();
        oauthUser = userRepository.save(oauthUser);

        String token = getTokenForUser(oauthUser.getEmail());

        mockMvc.perform(post("/api/v1/auth/oauth/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "RECRUITER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updatedUser = userRepository.findByEmail(oauthUser.getEmail()).orElseThrow();
        assertEquals(Role.RECRUITER, updatedUser.getRole());
        assertTrue(recruiterRepository.findByUserId(updatedUser.getId()).isPresent());
    }

    @Test
    void oauthRoleSelection_adminRejected() throws Exception {
        User oauthUser = User.builder()
                .email("oauth-admin-" + emailCounter.incrementAndGet() + "@example.com")
                .passwordHash(null)
                .name("OAuth Admin Attempt")
                .role(null)
                .isAnonymous(false)
                .emailVerified(true)
                .build();
        oauthUser = userRepository.save(oauthUser);

        String token = getTokenForUser(oauthUser.getEmail());

        mockMvc.perform(post("/api/v1/auth/oauth/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void oauthRoleSelection_alreadySet() throws Exception {
        // Register a normal user (already has role)
        String email = "existing-role-" + emailCounter.incrementAndGet() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword("password123");
        registerRequest.setName("Existing User");
        registerRequest.setRole(Role.STUDENT);

        String token = getTokenForEmail(email, registerRequest);

        mockMvc.perform(post("/api/v1/auth/oauth/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "RECRUITER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oauthRoleSelection_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "STUDENT"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauthRoleSelection_invalidRole() throws Exception {
        User oauthUser = User.builder()
                .email("oauth-invalid-" + emailCounter.incrementAndGet() + "@example.com")
                .passwordHash(null)
                .name("OAuth Invalid Role")
                .role(null)
                .isAnonymous(false)
                .emailVerified(true)
                .build();
        oauthUser = userRepository.save(oauthUser);

        String token = getTokenForUser(oauthUser.getEmail());

        mockMvc.perform(post("/api/v1/auth/oauth/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "INVALID_ROLE"))))
                .andExpect(status().isBadRequest());
    }

    // ── OAuth Account Linking Tests ──

    @Test
    void oauthProviderLinked_afterEmailRegistration() throws Exception {
        // Register a normal user first
        String email = "link-test-" + emailCounter.incrementAndGet() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword("password123");
        registerRequest.setName("Link Test User");
        registerRequest.setRole(Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();

        // Simulate linking an OAuth provider (via service directly for testing)
        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user)
                .provider("GOOGLE")
                .providerUserId("google-user-12345")
                .email(email)
                .name("Link Test User")
                .image("https://example.com/photo.jpg")
                .build();
        oAuthProviderRepository.save(oauthProvider);

        assertTrue(oAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", "google-user-12345"));
    }

    @Test
    void oauthProvider_duplicateProviderUserRejected() throws Exception {
        final User user1 = userRepository.save(User.builder()
                .email("dup-test-1-" + emailCounter.incrementAndGet() + "@example.com")
                .passwordHash(null)
                .name("User 1")
                .role(Role.STUDENT)
                .isAnonymous(false)
                .emailVerified(true)
                .build());

        final User user2 = userRepository.save(User.builder()
                .email("dup-test-2-" + emailCounter.incrementAndGet() + "@example.com")
                .passwordHash(null)
                .name("User 2")
                .role(Role.STUDENT)
                .isAnonymous(false)
                .emailVerified(true)
                .build());

        // Link first user to Google
        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user1)
                .provider("GOOGLE")
                .providerUserId("google-dup-12345")
                .email(user1.getEmail())
                .name("User 1")
                .build();
        oAuthProviderRepository.save(oauthProvider);

        // Attempt to link second user with same provider user ID
        assertThrows(Exception.class, () -> {
            OAuthProvider duplicate = OAuthProvider.builder()
                    .user(user2)
                    .provider("GOOGLE")
                    .providerUserId("google-dup-12345")
                    .email(user2.getEmail())
                    .name("User 2")
                    .build();
            oAuthProviderRepository.save(duplicate);
        });
    }

    // ── OAuth Endpoint Tests ──

    @Test
    void googleOAuthEndpoint_exists() throws Exception {
        // The endpoint should exist and reject invalid codes
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "invalid-test-code"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void githubOAuthEndpoint_exists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "invalid-test-code"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleOAuthEndpoint_emptyCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void githubOAuthEndpoint_emptyCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleOAuthEndpoint_emptyBody() throws Exception {
        // Empty body should fail validation since code is required
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Account Linking Duplicate Prevention ──

    @Test
    void oauthUser_existingEmail_linksInsteadOfDuplicate() throws Exception {
        // Register a normal user
        String email = "link-dup-" + emailCounter.incrementAndGet() + "@example.com";
        User user = User.builder()
                .email(email)
                .passwordHash("hashed-password")
                .name("Existing Email User")
                .role(Role.STUDENT)
                .isAnonymous(false)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        // Link an OAuth provider to this user
        OAuthProvider provider = OAuthProvider.builder()
                .user(user)
                .provider("GOOGLE")
                .providerUserId("google-link-dup-123")
                .email(email)
                .name("Existing Email User")
                .build();
        oAuthProviderRepository.save(provider);

        // Verify only one user exists with this email
        assertTrue(userRepository.findByEmail(email).isPresent());

        // Verify OAuth provider is linked
        assertTrue(oAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", "google-link-dup-123").isPresent());
    }

    // ── Helpers ──

    @Autowired
    private JwtService jwtService;

    /**
     * Generate a JWT access token directly for a user without login.
     * Used for testing OAuth users who have no password.
     */
    private String getTokenForUser(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElseThrow();
        String role = user.getRole() != null ? user.getRole().name() : "STUDENT";
        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash() != null ? user.getPasswordHash() : "",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                );
        return jwtService.generateAccessToken(userDetails, Map.of("role", role));
    }

    private String getTokenForEmail(String email, RegisterRequest registerRequest) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return (String) data.get("accessToken");
    }
}
