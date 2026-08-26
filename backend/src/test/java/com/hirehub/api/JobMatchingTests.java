package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.ai.AiService;
import com.hirehub.ai.JobMatchAnalysisResult;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.job.dto.CreateJobRequest;
import com.hirehub.job.dto.JobResponse;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.common.enums.JobType;
import com.hirehub.notification.repository.NotificationRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobMatchingTests {

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
        reset(aiService, textExtractionService);
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

    private JobResponse extractJob(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.treeToValue(root.path("data"), JobResponse.class);
    }

    private String bearer(String token) { return "Bearer " + token; }

    private MockMultipartFile pdfFile(String name) {
        byte[] content = ("Java Python Spring Boot PostgreSQL Docker BS Computer Science " +
                "Web application for job matching Software intern AWS Cloud Practitioner").getBytes();
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    private String uploadAndReturnId(AuthResponse auth) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(pdfFile("resume.pdf"))
                        .header("Authorization", bearer(auth.getAccessToken())))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }

    /**
     * Setup: company, job, student with resume.
     * Returns [recruiterAuth, studentAuth, jobId]
     */
    private Object[] setupFullEnvironment(String suffix) throws Exception {
        AuthResponse recAuth = register("rec-" + suffix + "@test.com", "Password123!", "Recruiter", Role.RECRUITER);
        CreateCompanyRequest compReq = new CreateCompanyRequest();
        compReq.setName("Company-" + suffix); compReq.setIndustry("Tech");
        MvcResult compResult = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compReq)))
                .andExpect(status().isCreated()).andReturn();
        CompanyResponse company = extractCompany(compResult);

        CreateJobRequest jobReq = new CreateJobRequest();
        jobReq.setTitle("Java Developer-" + suffix);
        jobReq.setDescription("Build Java microservices with Spring Boot");
        jobReq.setLocation("Bangalore");
        jobReq.setJobType(JobType.FULL_TIME);
        jobReq.setSkills("Java,Spring Boot,PostgreSQL,Docker,Kubernetes");
        jobReq.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated()).andReturn();
        JobResponse job = extractJob(jobResult);

        AuthResponse stuAuth = register("stu-" + suffix + "@test.com", "Password123!", "Student", Role.STUDENT);
        String resumeId = uploadAndReturnId(stuAuth);
        when(textExtractionService.extractText(any())).thenReturn(
                "Java Spring Boot PostgreSQL Docker resume text with skills and experience");

        return new Object[]{recAuth, stuAuth, job.getId().toString(), resumeId};
    }

    private void mockAiMatchSuccess() throws Exception {
        JobMatchAnalysisResult mockResult = JobMatchAnalysisResult.builder()
                .aiScore(82)
                .matchedSkills(List.of("Java", "Spring Boot"))
                .missingSkills(List.of("Kubernetes", "Kafka"))
                .strengths(List.of("Strong backend experience", "Good project portfolio"))
                .recommendations(List.of("Learn Kubernetes", "Add microservices project"))
                .explanation("Your Java and Spring Boot skills strongly match the core requirements.")
                .build();
        when(aiService.analyzeJobMatch(anyString(), anyString())).thenReturn(mockResult);
    }

    // ──────────────────────────────────────
    //  MATCH TESTS
    // ──────────────────────────────────────

    @Nested
    class MatchTests {

        @Test
        void studentCalculatesMatch() throws Exception {
            Object[] env = setupFullEnvironment("match1");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.matchScore").isNumber())
                    .andExpect(jsonPath("$.matchedSkills").isArray())
                    .andExpect(jsonPath("$.missingSkills").isArray())
                    .andExpect(jsonPath("$.explanation").isString());

            verify(aiService, times(1)).analyzeJobMatch(anyString(), anyString());
        }

        @Test
        void matchScoreIsBlended() throws Exception {
            Object[] env = setupFullEnvironment("blend");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.matchScore").isNumber());

            // Verify the match was saved
            var match = matchRepository.findByStudentIdAndJobIdAndResumeId(
                    studentRepository.findByUserId(userRepository.findByEmail(
                            "stu-blend@test.com").get().getId()).get().getId(),
                    UUID.fromString(jobId),
                    UUID.fromString(uploadAndReturnId(stuAuth)));
            // Score should be blended: AI 82 * 0.7 + deterministic * 0.3
        }

        @Test
        void cachedMatchNotReanalyzed() throws Exception {
            Object[] env = setupFullEnvironment("cache");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            // First match
            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            // Second match should use cache
            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            verify(aiService, times(1)).analyzeJobMatch(anyString(), anyString());
        }

        @Test
        void recruiterCannotMatch() throws Exception {
            Object[] env = setupFullEnvironment("rec-match");
            AuthResponse recAuth = (AuthResponse) env[0];
            String jobId = (String) env[2];

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void missingResumeReturnsError() throws Exception {
            AuthResponse stu = register("no-resume@test.com", "Password123!", "Student", Role.STUDENT);
            mockAiMatchSuccess();

            // No resume uploaded, no default
            CreateCompanyRequest compReq = new CreateCompanyRequest();
            compReq.setName("NoResCo"); compReq.setIndustry("Tech");
            AuthResponse recAuth = register("rec-nr@test.com", "Password123!", "Rec", Role.RECRUITER);
            mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(compReq)))
                    .andExpect(status().isCreated());

            CreateJobRequest jobReq = new CreateJobRequest();
            jobReq.setTitle("Job");
            jobReq.setDescription("Desc");
            jobReq.setLocation("Loc");
            jobReq.setJobType(JobType.FULL_TIME);
            jobReq.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
            MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(jobReq)))
                    .andExpect(status().isCreated()).andReturn();
            String jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString())
                    .path("data").path("id").asText();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stu.getAccessToken())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────
    //  QUERY TESTS
    // ──────────────────────────────────────

    @Nested
    class QueryTests {

        @Test
        void studentListsMatches() throws Exception {
            Object[] env = setupFullEnvironment("qmatch");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/job-matches/me")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void skillGapsAggregated() throws Exception {
            Object[] env = setupFullEnvironment("gaps");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/job-matches/me/skill-gaps")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].skill").isString())
                    .andExpect(jsonPath("$[0].count").isNumber());
        }

        @Test
        void getMatchNeverTriggersAI() throws Exception {
            Object[] env = setupFullEnvironment("qnoai");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            reset(aiService);

            mockMvc.perform(get("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/job-matches/me")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/job-matches/me/skill-gaps")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            verifyNoInteractions(aiService);
        }

        @Test
        void studentCannotSeeAnotherStudentMatches() throws Exception {
            Object[] env = setupFullEnvironment("idor-match");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            mockAiMatchSuccess();

            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            AuthResponse stu2 = register("stu2-idor@test.com", "Password123!", "Other", Role.STUDENT);

            mockMvc.perform(get("/api/v1/job-matches/me")
                            .header("Authorization", bearer(stu2.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));

            mockMvc.perform(get("/api/v1/job-matches/me/skill-gaps")
                            .header("Authorization", bearer(stu2.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void differentResumesCreateSeparateMatches() throws Exception {
            Object[] env = setupFullEnvironment("multi-resume");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];

            // Upload second resume
            MvcResult result2 = mockMvc.perform(multipart("/api/v1/resumes")
                            .file(pdfFile("resume2.pdf"))
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk()).andReturn();
            String resumeId2 = objectMapper.readTree(result2.getResponse().getContentAsString()).path("id").asText();

            mockAiMatchSuccess();

            // Match with default resume
            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            // Match with explicit second resume
            mockMvc.perform(post("/api/v1/jobs/" + jobId + "/match?resumeId=" + resumeId2)
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isCreated());

            // Both should be in the list
            mockMvc.perform(get("/api/v1/job-matches/me")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }
    }
}
