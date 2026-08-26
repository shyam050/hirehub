package com.hirehub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.auth.dto.LoginRequest;
import com.hirehub.auth.dto.RefreshTokenRequest;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.repository.StudentRepository;
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

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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
        recruiterRepository.deleteAll();
        companyRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        emailCounter.set(0);
    }

    // ── Registration ──

    @Test
    void registerStudent_returnsTokenAndUser() throws Exception {
        RegisterRequest req = buildRegister("student@test.com", "Password123!", "Test Student", Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("student@test.com"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.id").isNotEmpty());
    }

    @Test
    void registerRecruiter_returnsTokenAndUser() throws Exception {
        RegisterRequest req = buildRegister("recruiter@test.com", "Password123!", "Test Recruiter", Role.RECRUITER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("RECRUITER"));
    }

    @Test
    void registerAdmin_rejected() throws Exception {
        RegisterRequest req = buildRegister("admin@test.com", "Password123!", "Fake Admin", Role.ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void registerDuplicateEmail_returns409() throws Exception {
        RegisterRequest req = buildRegister("dup@test.com", "Password123!", "First User", Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        req.setName("Second User");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void registerInvalidEmail_returns400() throws Exception {
        RegisterRequest req = buildRegister("not-an-email", "Password123!", "Test", Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerShortPassword_returns400() throws Exception {
        RegisterRequest req = buildRegister("short@test.com", "1234", "Test", Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── Login ──

    @Test
    void loginValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        buildRegister("login@test.com", "Password123!", "Login User", Role.STUDENT))));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("login@test.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("login@test.com"));
    }

    @Test
    void loginInvalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        buildRegister("wrongpw@test.com", "Password123!", "Wrong PW User", Role.STUDENT))));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("wrongpw@test.com");
        loginReq.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginNonexistentEmail_returns401() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("nobody@test.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    // ── Protected endpoints ──

    @Test
    void accessProtectedEndpointWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpointWithValidToken_returns200() throws Exception {
        String accessToken = registerAndGetAccessToken("me@test.com");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me@test.com"));
    }

    @Test
    void accessProtectedEndpointWithInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpointWithExpiredToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiZXhwIjoxfQ.invalid"))
                .andExpect(status().isUnauthorized());
    }

    // ── Refresh token ──

    @Test
    void refreshWithValidToken_returnsNewPair() throws Exception {
        String refreshToken = registerAndGetRefreshToken("refresh@test.com");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void refreshWithRevokedToken_returns401() throws Exception {
        String refreshToken = registerAndGetRefreshToken("revoke@test.com");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(refreshToken);

        // First refresh — rotates token
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Second refresh with same old token — revoked
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithInvalidToken_returns401() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("completely-fake-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── Logout ──

    @Test
    void logout_revokesRefreshTokens() throws Exception {
        String[] tokens = registerAndGetBothTokens("logout@test.com");
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Refresh should fail after logout
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── Role-based access ──

    @Test
    void studentHasCorrectRole() throws Exception {
        String token = registerAndGetAccessToken("rolecheck@test.com");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    // ── Helpers ──

    private RegisterRequest buildRegister(String email, String password, String name, Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setName(name);
        req.setRole(role);
        return req;
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRegister(email, "Password123!", "Test User", Role.STUDENT))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    private String extractRefreshToken(MvcResult result) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if (setCookie == null) return null;
        for (String part : setCookie.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("refresh_token=")) {
                return trimmed.substring("refresh_token=".length());
            }
        }
        return null;
    }

    private String registerAndGetRefreshToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRegister(email, "Password123!", "Test User", Role.STUDENT))))
                .andReturn();

        return extractRefreshToken(result);
    }

    private String[] registerAndGetBothTokens(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRegister(email, "Password123!", "Test User", Role.STUDENT))))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String access = objectMapper.readTree(body).path("data").path("accessToken").asText();
        String refresh = extractRefreshToken(result);
        return new String[]{access, refresh};
    }
}
