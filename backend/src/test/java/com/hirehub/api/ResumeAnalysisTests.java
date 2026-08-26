package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.ai.AiService;
import com.hirehub.ai.ResumeAnalysisResult;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.io.InputStream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumeAnalysisTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private ResumeAnalysisRepository analysisRepository;
    @Autowired private JobMatchRepository matchRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private com.hirehub.resume.text.ResumeTextExtractionService textExtractionService;

    @BeforeEach
    void cleanDatabase() {
        matchRepository.deleteAll();
        analysisRepository.deleteAll();
        interviewRepository.deleteAll();
        notificationRepository.deleteAll();
        applicationRepository.deleteAll();
        resumeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        jobRepository.deleteAll();
        recruiterRepository.deleteAll();
        companyRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        reset(aiService);
    }

    // ── Helpers ──

    private AuthResponse register(String email, String password, String name, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email); req.setPassword(password); req.setName(name); req.setRole(role);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return extractAuth(result);
    }

    private AuthResponse extractAuth(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.treeToValue(root.path("data"), AuthResponse.class);
    }

    private CompanyResponse extractCompany(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.treeToValue(root.path("data"), CompanyResponse.class);
    }

    private String bearer(String token) { return "Bearer " + token; }

    private User createAdmin(String email) {
        return userRepository.save(User.builder()
                .email(email).name("Admin").role(Role.ADMIN)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .build());
    }

    private MockMultipartFile pdfFile(String name) {
        byte[] content = "%PDF-1.4\nThis is a test resume with skills in Java, Python, and Spring Boot.\nEducation: BS Computer Science.\nProjects: Web application for job matching.\nExperience: Software intern at TechCorp.\nCertifications: AWS Cloud Practitioner.\nAchievements: Dean's list.\nEND".getBytes();
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    private String uploadAndReturnId(AuthResponse auth) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(pdfFile("resume.pdf"))
                        .header("Authorization", bearer(auth.getAccessToken())))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }

    private void mockAiSuccess() throws Exception {
        ResumeAnalysisResult mockResult = ResumeAnalysisResult.builder()
                .overallScore(78)
                .extractedSkills(List.of("Java", "Python", "Spring Boot"))
                .extractedEducation(List.of("BS Computer Science"))
                .extractedProjects(List.of("Web application for job matching"))
                .extractedExperience(List.of("Software intern at TechCorp"))
                .extractedCertifications(List.of("AWS Cloud Practitioner"))
                .extractedAchievements(List.of("Dean's list"))
                .strengths(List.of("Strong technical skills", "Good project experience"))
                .weaknesses(List.of("Limited work experience"))
                .missingSkills(List.of("Kubernetes", "Docker"))
                .recommendations(List.of("Add more quantifiable results", "Include metrics"))
                .build();
        when(aiService.analyzeResume(anyString())).thenReturn(mockResult);
        when(textExtractionService.extractText(any())).thenReturn(
                "Java Python Spring Boot BS Computer Science Web application Software intern AWS Cloud Practitioner Deans list");
    }

    // ──────────────────────────────────────
    //  ANALYZE TESTS
    // ──────────────────────────────────────

    @Nested
    class AnalyzeTests {

        @Test
        void studentAnalyzesResume() throws Exception {
            AuthResponse auth = register("an1@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            mockAiSuccess();

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.overallScore").value(78))
                    .andExpect(jsonPath("$.extractedSkills[0]").value("Java"))
                    .andExpect(jsonPath("$.strengths[0]").value("Strong technical skills"));

            verify(aiService, times(1)).analyzeResume(anyString());
        }

        @Test
        void studentCannotAnalyzeOtherStudentResume() throws Exception {
            AuthResponse s1 = register("s1a@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2a@test.com", "Password123!", "S2", Role.STUDENT);
            String resumeId = uploadAndReturnId(s2);

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotAnalyzeResume() throws Exception {
            AuthResponse rec = register("rec-an@test.com", "Password123!", "Rec", Role.RECRUITER);

            mockMvc.perform(post("/api/v1/resumes/" + UUID.randomUUID() + "/analyze")
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedCannotAnalyze() throws Exception {
            mockMvc.perform(post("/api/v1/resumes/" + UUID.randomUUID() + "/analyze"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void missingResumeReturns404() throws Exception {
            AuthResponse auth = register("an404@test.com", "Password123!", "Student", Role.STUDENT);
            String fakeId = UUID.randomUUID().toString();

            mockMvc.perform(post("/api/v1/resumes/" + fakeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isNotFound());
        }

        @Test
        void aiFailureReturnsError() throws Exception {
            AuthResponse auth = register("anfail@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            when(textExtractionService.extractText(any())).thenReturn("Some extracted text");
            when(aiService.analyzeResume(anyString()))
                    .thenThrow(new com.hirehub.ai.AiServiceException("AI service unavailable"));

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void cachedAnalysisNotReanalyzed() throws Exception {
            AuthResponse auth = register("ancache@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            mockAiSuccess();

            // First analysis
            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated());

            // Second analysis should use cache
            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated());

            // AI should only be called once
            verify(aiService, times(1)).analyzeResume(anyString());
        }
    }

    // ──────────────────────────────────────
    //  QUERY TESTS
    // ──────────────────────────────────────

    @Nested
    class QueryTests {

        @Test
        void studentListsAnalysesForResume() throws Exception {
            AuthResponse auth = register("ql1@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            mockAiSuccess();

            // Create an analysis
            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/analyses")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void studentGetsLatestAnalysis() throws Exception {
            AuthResponse auth = register("ql2@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            mockAiSuccess();

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/analyses/latest")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallScore").value(78));
        }

        @Test
        void studentListsAllMyAnalyses() throws Exception {
            AuthResponse auth = register("ql3@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            mockAiSuccess();

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/resume-analyses/me")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].overallScore").value(78));
        }

        @Test
        void getAnalysesNeverTriggersAI() throws Exception {
            AuthResponse auth = register("ql4@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(auth);
            mockAiSuccess();

            // Create analysis once
            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isCreated());

            reset(aiService); // Reset mock to verify no more calls

            // GET endpoints must never trigger AI
            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/analyses")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/analyses/latest")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/resume-analyses/me")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk());

            verifyNoInteractions(aiService);
        }

        @Test
        void studentCannotSeeAnotherStudentAnalyses() throws Exception {
            AuthResponse s1 = register("s1ql@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2ql@test.com", "Password123!", "S2", Role.STUDENT);
            String resumeId = uploadAndReturnId(s2);
            mockAiSuccess();

            // S2 analyzes
            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(s2.getAccessToken())))
                    .andExpect(status().isCreated());

            // S1 cannot see S2's analyses
            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/analyses")
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/resume-analyses/me")
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        void adminCanAccessAnalyses() throws Exception {
            AuthResponse stu = register("stu-admin-ql@test.com", "Password123!", "Student", Role.STUDENT);
            String resumeId = uploadAndReturnId(stu);
            mockAiSuccess();

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                            .header("Authorization", bearer(stu.getAccessToken())))
                    .andExpect(status().isCreated());

            // Create admin directly and login
            User admin = createAdmin("admin-ql@test.com");
            com.hirehub.auth.dto.LoginRequest loginReq = new com.hirehub.auth.dto.LoginRequest();
            loginReq.setEmail("admin-ql@test.com");
            loginReq.setPassword("Password123!");
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk()).andReturn();
            JsonNode loginRoot = objectMapper.readTree(loginResult.getResponse().getContentAsString());
            AuthResponse adminAuthResp = objectMapper.treeToValue(loginRoot.path("data"), AuthResponse.class);

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/analyses")
                            .header("Authorization", bearer(adminAuthResp.getAccessToken())))
                    .andExpect(status().isOk());
        }
    }
}
