package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.ai.*;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.*;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.job.dto.CreateJobRequest;
import com.hirehub.job.dto.JobResponse;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.aiinterview.repository.AiInterviewRepository;
import com.hirehub.aiinterview.repository.AiInterviewQuestionRepository;
import com.hirehub.aiinterview.dto.StartInterviewRequest;
import com.hirehub.aiinterview.dto.SubmitAnswerRequest;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class AiInterviewTests {

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
    @Autowired private AiInterviewRepository aiInterviewRepository;
    @Autowired private AiInterviewQuestionRepository questionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private com.hirehub.resume.text.ResumeTextExtractionService textExtractionService;

    @BeforeEach
    void cleanDatabase() {
        questionRepository.deleteAll();
        aiInterviewRepository.deleteAll();
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
        byte[] content = ("Java Spring Boot PostgreSQL Docker resume text with skills and experience").getBytes();
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    private String uploadAndReturnId(AuthResponse auth) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(pdfFile("resume.pdf"))
                        .header("Authorization", bearer(auth.getAccessToken())))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }

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
        jobReq.setDescription("Build Java microservices");
        jobReq.setLocation("Bangalore");
        jobReq.setJobType(JobType.FULL_TIME);
        jobReq.setSkills("Java,Spring Boot,PostgreSQL,Docker");
        jobReq.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated()).andReturn();
        JobResponse job = extractJob(jobResult);

        AuthResponse stuAuth = register("stu-" + suffix + "@test.com", "Password123!", "Student", Role.STUDENT);
        String resumeId = uploadAndReturnId(stuAuth);
        when(textExtractionService.extractText(any())).thenReturn("Java Spring Boot PostgreSQL Docker resume text");

        return new Object[]{recAuth, stuAuth, job.getId().toString(), resumeId};
    }

    private void mockQuestionGeneration() throws Exception {
        GeneratedQuestion q = GeneratedQuestion.builder()
                .question("What is dependency injection in Spring?")
                .category("technical")
                .expectedTopics(List.of("IoC", "beans", "annotations"))
                .build();
        when(aiService.generateMockInterviewQuestion(
                anyString(), anyString(), anyList(), anyString(), anyList(),
                anyString(), anyString(), anyInt(), anyInt(),
                anyList(), any(), any())).thenReturn(q);
    }

    private void mockAnswerEvaluation() throws Exception {
        AnswerEvaluation eval = AnswerEvaluation.builder()
                .score(75)
                .strengths(List.of("Good understanding"))
                .weaknesses(List.of("Could mention examples"))
                .feedback("Solid answer with room for improvement.")
                .missingConcepts(List.of("Constructor injection"))
                .idealAnswerPoints(List.of("Explain IoC container", "Mention @Autowired"))
                .build();
        when(aiService.evaluateMockInterviewAnswer(
                anyString(), anyString(), anyList(), anyString(),
                anyString(), anyList(), anyString())).thenReturn(eval);
    }

    private void mockReportGeneration() throws Exception {
        InterviewReport report = InterviewReport.builder()
                .overallScore(75)
                .technicalScore(80)
                .communicationScore(70)
                .problemSolvingScore(72)
                .strongestAreas(List.of("Spring Boot", "REST APIs"))
                .weakestAreas(List.of("System design"))
                .missingConcepts(List.of("Microservices patterns"))
                .recommendedTopics(List.of("Design patterns", "System design"))
                .overallFeedback("Good technical foundation with room to grow.")
                .build();
        when(aiService.generateMockInterviewReport(
                anyString(), anyString(), anyList())).thenReturn(report);
    }

    // ──────────────────────────────────────
    //  START INTERVIEW TESTS
    // ──────────────────────────────────────

    @Nested
    class StartTests {

        @Test
        void studentStartsInterview() throws Exception {
            Object[] env = setupFullEnvironment("start1");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];
            mockQuestionGeneration();

            StartInterviewRequest req = new StartInterviewRequest();
            req.setJobId(UUID.fromString(jobId));
            req.setResumeId(UUID.fromString(resumeId));
            req.setInterviewType(AiInterviewType.TECHNICAL);
            req.setDifficulty(AiInterviewDifficulty.MEDIUM);
            req.setTotalQuestions("5");

            mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.totalQuestions").value(5))
                    .andExpect(jsonPath("$.currentQuestionNumber").value(1))
                    .andExpect(jsonPath("$.interviewType").value("TECHNICAL"));

            verify(aiService, times(1)).generateMockInterviewQuestion(
                    anyString(), anyString(), anyList(), anyString(), anyList(),
                    anyString(), anyString(), anyInt(), anyInt(),
                    anyList(), any(), any());
        }

        @Test
        void recruiterCannotStartInterview() throws Exception {
            Object[] env = setupFullEnvironment("rec-start");
            AuthResponse recAuth = (AuthResponse) env[0];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            StartInterviewRequest req = new StartInterviewRequest();
            req.setJobId(UUID.fromString(jobId));
            req.setResumeId(UUID.fromString(resumeId));
            req.setInterviewType(AiInterviewType.HR);
            req.setTotalQuestions("5");

            mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void cannotUseOtherStudentsResume() throws Exception {
            Object[] env = setupFullEnvironment("idor-resume");
            AuthResponse stu1 = register("s1-idor@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse stu2 = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            StartInterviewRequest req = new StartInterviewRequest();
            req.setJobId(UUID.fromString(jobId));
            req.setResumeId(UUID.fromString(resumeId));
            req.setInterviewType(AiInterviewType.TECHNICAL);
            req.setTotalQuestions("5");

            mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stu1.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  QUESTION + ANSWER TESTS
    // ──────────────────────────────────────

    @Nested
    class QuestionAnswerTests {

        @Test
        void fullInterviewFlow() throws Exception {
            Object[] env = setupFullEnvironment("flow");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            mockQuestionGeneration();
            mockAnswerEvaluation();
            mockReportGeneration();

            // Start (generates Q1)
            StartInterviewRequest startReq = new StartInterviewRequest();
            startReq.setJobId(UUID.fromString(jobId));
            startReq.setResumeId(UUID.fromString(resumeId));
            startReq.setInterviewType(AiInterviewType.TECHNICAL);
            startReq.setTotalQuestions("5");

            MvcResult startResult = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated()).andReturn();

            String interviewId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                    .path("id").asText();

            // Answer Q1
            SubmitAnswerRequest ansReq = new SubmitAnswerRequest();
            ansReq.setStudentAnswer("Dependency injection is a design pattern where...");

            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/questions/1/answer")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ansReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.score").value(75));

            // Generate Q2
            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/next-question")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionNumber").value(2));

            // Answer Q2-Q5 and generate Q3-Q5
            for (int i = 2; i <= 5; i++) {
                mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/questions/" + i + "/answer")
                                .header("Authorization", bearer(stuAuth.getAccessToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ansReq)))
                        .andExpect(status().isOk());

                if (i < 5) {
                    mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/next-question")
                                    .header("Authorization", bearer(stuAuth.getAccessToken())))
                            .andExpect(status().isOk());
                }
            }

            // Complete
            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.overallScore").isNumber())
                    .andExpect(jsonPath("$.completedAt").isString());
        }

        @Test
        void cannotAnswerCompletedInterview() throws Exception {
            Object[] env = setupFullEnvironment("no-ans");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            mockQuestionGeneration();
            mockAnswerEvaluation();
            mockReportGeneration();

            // Create + complete a quick interview
            StartInterviewRequest startReq = new StartInterviewRequest();
            startReq.setJobId(UUID.fromString(jobId));
            startReq.setResumeId(UUID.fromString(resumeId));
            startReq.setInterviewType(AiInterviewType.HR);
            startReq.setTotalQuestions("5");

            MvcResult startResult = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(startResult.getResponse().getContentAsString()).path("id").asText();

            SubmitAnswerRequest ansReq = new SubmitAnswerRequest();
            ansReq.setStudentAnswer("My answer");

            // Generate and answer all 5 questions
            for (int i = 1; i <= 5; i++) {
                if (i > 1) {
                    mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/next-question")
                                    .header("Authorization", bearer(stuAuth.getAccessToken())))
                            .andExpect(status().isOk());
                }
                mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/questions/" + i + "/answer")
                                .header("Authorization", bearer(stuAuth.getAccessToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ansReq)))
                        .andExpect(status().isOk());
            }

            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            // Cannot answer after completion
            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/questions/1/answer")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ansReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void cannotAnswerAnotherStudentsQuestion() throws Exception {
            Object[] env = setupFullEnvironment("idor-ans");
            AuthResponse stu1 = register("s1-ans@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse stu2 = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            mockQuestionGeneration();

            StartInterviewRequest startReq = new StartInterviewRequest();
            startReq.setJobId(UUID.fromString(jobId));
            startReq.setResumeId(UUID.fromString(resumeId));
            startReq.setInterviewType(AiInterviewType.TECHNICAL);
            startReq.setTotalQuestions("5");

            MvcResult startResult = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stu2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(startResult.getResponse().getContentAsString()).path("id").asText();

            SubmitAnswerRequest ansReq = new SubmitAnswerRequest();
            ansReq.setStudentAnswer("My answer");

            // Student 1 tries to answer student 2's question
            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/questions/1/answer")
                            .header("Authorization", bearer(stu1.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ansReq)))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  ABANDON TESTS
    // ──────────────────────────────────────

    @Nested
    class AbandonTests {

        @Test
        void studentAbandonsInterview() throws Exception {
            Object[] env = setupFullEnvironment("abandon");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            mockQuestionGeneration();

            StartInterviewRequest startReq = new StartInterviewRequest();
            startReq.setJobId(UUID.fromString(jobId));
            startReq.setResumeId(UUID.fromString(resumeId));
            startReq.setInterviewType(AiInterviewType.MIXED);
            startReq.setTotalQuestions("5");

            MvcResult startResult = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(startResult.getResponse().getContentAsString()).path("id").asText();

            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/abandon")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ABANDONED"));
        }

        @Test
        void cannotAbandonCompletedInterview() throws Exception {
            Object[] env = setupFullEnvironment("no-abandon");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String jobId = (String) env[2];
            String resumeId = (String) env[3];

            mockQuestionGeneration();
            mockAnswerEvaluation();
            mockReportGeneration();

            StartInterviewRequest startReq = new StartInterviewRequest();
            startReq.setJobId(UUID.fromString(jobId));
            startReq.setResumeId(UUID.fromString(resumeId));
            startReq.setInterviewType(AiInterviewType.TECHNICAL);
            startReq.setTotalQuestions("5");

            MvcResult startResult = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(startResult.getResponse().getContentAsString()).path("id").asText();

            SubmitAnswerRequest ansReq = new SubmitAnswerRequest();
            ansReq.setStudentAnswer("Answer");

            // Generate and answer all 5 questions
            for (int i = 1; i <= 5; i++) {
                if (i > 1) {
                    mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/next-question")
                                    .header("Authorization", bearer(stuAuth.getAccessToken())))
                            .andExpect(status().isOk());
                }
                mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/questions/" + i + "/answer")
                                .header("Authorization", bearer(stuAuth.getAccessToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ansReq)))
                        .andExpect(status().isOk());
            }

            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/ai-interviews/" + interviewId + "/abandon")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────
    //  QUERY TESTS
    // ──────────────────────────────────────

    @Nested
    class QueryTests {

        @Test
        void studentListsInterviews() throws Exception {
            Object[] env = setupFullEnvironment("ql");
            AuthResponse stuAuth = (AuthResponse) env[1];
            mockQuestionGeneration();

            StartInterviewRequest req = new StartInterviewRequest();
            req.setJobId(UUID.fromString((String) env[2]));
            req.setResumeId(UUID.fromString((String) env[3]));
            req.setInterviewType(AiInterviewType.TECHNICAL);
            req.setTotalQuestions("5");

            mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/ai-interviews/me")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void getInterviewNeverCallsAI() throws Exception {
            Object[] env = setupFullEnvironment("ql-noai");
            AuthResponse stuAuth = (AuthResponse) env[1];
            mockQuestionGeneration();

            StartInterviewRequest req = new StartInterviewRequest();
            req.setJobId(UUID.fromString((String) env[2]));
            req.setResumeId(UUID.fromString((String) env[3]));
            req.setInterviewType(AiInterviewType.TECHNICAL);
            req.setTotalQuestions("5");

            MvcResult result = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated()).andReturn();
            String id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            reset(aiService);

            mockMvc.perform(get("/api/v1/ai-interviews/" + id)
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/ai-interviews/" + id + "/questions")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/ai-interviews/" + id + "/report")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk());

            verifyNoInteractions(aiService);
        }

        @Test
        void cannotAccessAnotherStudentsInterview() throws Exception {
            Object[] env = setupFullEnvironment("idor-ql");
            AuthResponse stu1 = register("s1-ql@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse stu2 = (AuthResponse) env[1];
            mockQuestionGeneration();

            StartInterviewRequest req = new StartInterviewRequest();
            req.setJobId(UUID.fromString((String) env[2]));
            req.setResumeId(UUID.fromString((String) env[3]));
            req.setInterviewType(AiInterviewType.TECHNICAL);
            req.setTotalQuestions("5");

            MvcResult result = mockMvc.perform(post("/api/v1/ai-interviews")
                            .header("Authorization", bearer(stu2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated()).andReturn();
            String id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            mockMvc.perform(get("/api/v1/ai-interviews/" + id)
                            .header("Authorization", bearer(stu1.getAccessToken())))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/ai-interviews/" + id + "/questions")
                            .header("Authorization", bearer(stu1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }
    }
}
