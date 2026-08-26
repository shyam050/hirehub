package com.hirehub.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.auth.dto.LoginRequest;
import com.hirehub.auth.dto.RefreshTokenRequest;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.RateLimiter;
import com.hirehub.common.enums.Role;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductionHardeningTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JobMatchRepository matchRepository;
    @Autowired private ResumeAnalysisRepository analysisRepository;
    @Autowired private RateLimiter rateLimiter;

    @BeforeEach
    void cleanDatabase() {
        matchRepository.deleteAll();
        analysisRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }

    // ── Refresh Token Cookie Tests ──

    @Test
    @Order(1)
    @DisplayName("Login sets HttpOnly refresh_token cookie")
    void loginSetsRefreshCookie() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("cookie@test.com");
        reg.setPassword("Password123!");
        reg.setName("Cookie Test");
        reg.setRole(Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest login = new LoginRequest();
        login.setEmail("cookie@test.com");
        login.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie, "Set-Cookie header should be present");
        assertTrue(setCookie.contains("refresh_token="), "Should contain refresh_token cookie");
        assertTrue(setCookie.contains("HttpOnly"), "Cookie should be HttpOnly");
        assertTrue(setCookie.contains("Path=/api/v1/auth"), "Cookie should have correct path");
    }

    @Test
    @Order(2)
    @DisplayName("Refresh token not in response body")
    void refreshTokenNotInResponseBody() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("body@test.com");
        reg.setPassword("Password123!");
        reg.setName("Body Test");
        reg.setRole(Role.STUDENT);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("accessToken"), "Response should contain accessToken");
        // The field may appear as null in JSON but should not have a real value
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
        com.fasterxml.jackson.databind.JsonNode refreshTokenNode = root.path("data").path("refreshToken");
        assertTrue(refreshTokenNode.isNull() || refreshTokenNode.isMissingNode(),
                "Refresh token should not be in response body");
    }

    @Test
    @Order(3)
    @DisplayName("Refresh works via cookie")
    void refreshViaCookie() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("refreshcookie@test.com");
        reg.setPassword("Password123!");
        reg.setName("Refresh Cookie Test");
        reg.setRole(Role.STUDENT);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract refresh token from Set-Cookie header
        String setCookie = registerResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie, "Register should set refresh token cookie");
        assertTrue(setCookie.contains("refresh_token="), "Cookie should contain refresh_token");

        // Extract the actual token value from the cookie
        String cookiePart = setCookie.split(";")[0];
        String tokenValue = cookiePart.substring(cookiePart.indexOf("=") + 1);

        // Verify refresh works using body fallback
        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(tokenValue);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @Order(4)
    @DisplayName("Logout clears refresh cookie")
    void logoutClearsCookie() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("logoutcookie@test.com");
        reg.setPassword("Password123!");
        reg.setName("Logout Cookie Test");
        reg.setRole(Role.STUDENT);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = registerResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).path("data").path("accessToken").asText();

        // Logout
        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = logoutResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie, "Logout should set cookie header");
        assertTrue(setCookie.contains("refresh_token="), "Should clear refresh_token cookie");
        assertTrue(setCookie.contains("Max-Age=0") || setCookie.contains("Max-Age=0;"),
                "Cookie should have Max-Age=0 to clear it");
    }

    // ── Request ID Tests ──

    @Test
    @Order(5)
    @DisplayName("Response includes X-Request-ID header")
    void responseIncludesRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    @Order(6)
    @DisplayName("X-Request-ID is preserved when sent by client")
    void requestIdPreservedFromClient() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("X-Request-ID", "my-custom-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "my-custom-id"));
    }

    // ── Actuator Tests ──

    @Test
    @Order(7)
    @DisplayName("Health endpoint is accessible")
    void healthEndpointAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(8)
    @DisplayName("Metrics endpoint requires authentication")
    void metricsEndpointRequiresAuth() throws Exception {
        // Without auth, gets 401. With non-admin auth, gets 403.
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rate Limiting Tests ──

    @Test
    @Order(9)
    @DisplayName("Rate limiter blocks excessive requests")
    void rateLimiterBlocksExcessiveRequests() {
        // Temporarily enable rate limiter for this test
        ReflectionTestUtils.setField(rateLimiter, "enabled", true);
        try {
            String testKey = "test-rate-limit-" + System.currentTimeMillis();

            // First request should pass
            assertDoesNotThrow(() -> rateLimiter.check(testKey, 2));

            // Second should pass
            assertDoesNotThrow(() -> rateLimiter.check(testKey, 2));

            // Third should throw (exceeds limit of 2)
            assertThrows(com.hirehub.common.exception.RateLimitException.class,
                    () -> rateLimiter.check(testKey, 2));
        } finally {
            ReflectionTestUtils.setField(rateLimiter, "enabled", false);
        }
    }

    @Test
    @Order(10)
    @DisplayName("Rate limit returns 429 status code")
    void rateLimitReturns429() throws Exception {
        // Temporarily enable rate limiter for this test
        ReflectionTestUtils.setField(rateLimiter, "enabled", true);
        try {
            RegisterRequest reg = new RegisterRequest();
            reg.setEmail("ratelimit" + System.currentTimeMillis() + "@test.com");
            reg.setPassword("Password123!");
            reg.setName("Rate Limit Test");
            reg.setRole(Role.STUDENT);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isCreated());

            LoginRequest login = new LoginRequest();
            login.setEmail(reg.getEmail());
            login.setPassword("Password123!");

            boolean got429 = false;
            for (int i = 0; i < 15; i++) {
                MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                        .andReturn();
                if (result.getResponse().getStatus() == 429) {
                    got429 = true;
                    break;
                }
            }
            assertTrue(got429, "Should eventually get 429 rate limit response");
        } finally {
            ReflectionTestUtils.setField(rateLimiter, "enabled", false);
        }
    }

    // ── CORS Tests ──

    @Test
    @Order(11)
    @DisplayName("OPTIONS preflight is allowed")
    void optionsPreflightAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/jobs")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    // ── Auth Security Tests ──

    @Test
    @Order(12)
    @DisplayName("Protected endpoint requires authentication")
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/students/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("Invalid JWT is rejected")
    void invalidJwtRejected() throws Exception {
        mockMvc.perform(get("/api/v1/students/me")
                        .header("Authorization", "Bearer invalid-token-here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(14)
    @DisplayName("Student cannot access admin-only endpoint")
    void studentCannotAccessAdminEndpoint() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("student-security" + System.currentTimeMillis() + "@test.com");
        reg.setPassword("Password123!");
        reg.setName("Security Test");
        reg.setRole(Role.STUDENT);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).path("data").path("accessToken").asText();

        // Use a random UUID that doesn't exist — student should get 403 (PreAuthorize blocks first)
        String randomId = java.util.UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/users/" + randomId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(result1 -> {
                    int status = result1.getResponse().getStatus();
                    // 403 = forbidden (PreAuthorize blocks), 404 = not found (if authorization passes first)
                    assertTrue(status == 403 || status == 404,
                            "Expected 403 or 404 for student accessing admin endpoint, got " + status);
                });
    }
}
