package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.application.dto.CreateApplicationRequest;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.LoginRequest;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.*;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.interview.dto.*;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.job.dto.CreateJobRequest;
import com.hirehub.job.dto.JobResponse;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.aiinterview.repository.AiInterviewQuestionRepository;
import com.hirehub.aiinterview.repository.AiInterviewRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InterviewTests {

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
    @Autowired private JobMatchRepository matchRepository;
    @Autowired private AiInterviewQuestionRepository aiInterviewQuestionRepository;
    @Autowired private AiInterviewRepository aiInterviewRepository;
    @Autowired private ResumeAnalysisRepository analysisRepository;
    @Autowired private ResumeRepository resumeRepository; 
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        aiInterviewQuestionRepository.deleteAll();
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

    private User createAdmin(String email) {
        return userRepository.save(User.builder()
                .email(email).name("Admin").role(Role.ADMIN)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .build());
    }

    /**
     * Set up a complete environment: company, job, student, application.
     * Returns [recruiterAuth, studentAuth, jobId, applicationId]
     */
    private Object[] setupFullEnvironment(String suffix) throws Exception {
        // Register recruiter and create company
        AuthResponse recAuth = register("rec-" + suffix + "@test.com", "Password123!", "Recruiter", Role.RECRUITER);
        CreateCompanyRequest compReq = new CreateCompanyRequest();
        compReq.setName("Company-" + suffix);
        compReq.setIndustry("Tech");
        MvcResult compResult = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compReq)))
                .andExpect(status().isCreated()).andReturn();
        CompanyResponse company = extractCompany(compResult);

        // Create job
        CreateJobRequest jobReq = new CreateJobRequest();
        jobReq.setTitle("Software Engineer-" + suffix);
        jobReq.setDescription("Build software");
        jobReq.setLocation("Bangalore");
        jobReq.setJobType(JobType.FULL_TIME);
        // companyId is resolved server-side from recruiter profile
        jobReq.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated()).andReturn();
        JobResponse job = extractJob(jobResult);

        // Register student
        AuthResponse stuAuth = register("stu-" + suffix + "@test.com", "Password123!", "Student", Role.STUDENT);

        // Apply
        CreateApplicationRequest appReq = new CreateApplicationRequest();
        appReq.setCoverLetter("I am interested");
        MvcResult appResult = mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                        .header("Authorization", bearer(stuAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated()).andReturn();
        JsonNode appRoot = objectMapper.readTree(appResult.getResponse().getContentAsString());
        String applicationId = appRoot.path("data").path("id").asText();

        return new Object[]{recAuth, stuAuth, job.getId().toString(), applicationId};
    }

    // ──────────────────────────────────────
    //  INTERVIEW SCHEDULING TESTS
    // ──────────────────────────────────────

    @Nested
    class ScheduleTests {

        @Test
        void recruiterSchedulesInterview() throws Exception {
            Object[] env = setupFullEnvironment("sched1");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.TECHNICAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);
            req.setMeetingLink("https://meet.example.com/abc");
            req.setInterviewerName("John Smith");

            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.interviewType").value("TECHNICAL"))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.duration").value(60))
                    .andExpect(jsonPath("$.meetingLink").value("https://meet.example.com/abc"))
                    .andExpect(jsonPath("$.interviewerName").value("John Smith"));
        }

        @Test
        void studentCannotScheduleInterview() throws Exception {
            Object[] env = setupFullEnvironment("stu-sched");
            AuthResponse stuAuth = (AuthResponse) env[1];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.HR);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(45);

            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotScheduleForAnotherCompany() throws Exception {
            Object[] env = setupFullEnvironment("idor-sched");
            String applicationId = (String) env[3];

            // Register a different recruiter with a different company
            AuthResponse rec2 = register("rec2-idor@test.com", "Password123!", "Rec2", Role.RECRUITER);
            CreateCompanyRequest comp2 = new CreateCompanyRequest();
            comp2.setName("OtherCompany");
            comp2.setIndustry("Finance");
            mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comp2)))
                    .andExpect(status().isCreated());

            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.TECHNICAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);

            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedCannotSchedule() throws Exception {
            Object[] env = setupFullEnvironment("unauth-sched");
            String applicationId = (String) env[3];

            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.TECHNICAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);

            mockMvc.perform(post("/api/v1/interviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────
    //  STUDENT INTERVIEW ACCESS TESTS
    // ──────────────────────────────────────

    @Nested
    class StudentAccessTests {

        @Test
        void studentSeesOwnInterviews() throws Exception {
            Object[] env = setupFullEnvironment("stu-acc");
            AuthResponse recAuth = (AuthResponse) env[0];
            AuthResponse stuAuth = (AuthResponse) env[1];
            String applicationId = (String) env[3];

            // Schedule interview
            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.TECHNICAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);

            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            // Student sees their interview
            mockMvc.perform(get("/api/v1/interviews/me")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].interviewType").value("TECHNICAL"));
        }

        @Test
        void studentCannotSeeAnotherStudentsInterview() throws Exception {
            // Setup environment 1
            Object[] env1 = setupFullEnvironment("idor1");
            AuthResponse recAuth = (AuthResponse) env1[0];
            String applicationId1 = (String) env1[3];

            // Schedule interview for student 1
            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId1));
            req.setInterviewType(InterviewType.HR);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(45);

            MvcResult intResult = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(intResult.getResponse().getContentAsString())
                    .path("id").asText();

            // Register a second student
            AuthResponse stu2 = register("stu-idor2@test.com", "Password123!", "Other", Role.STUDENT);

            // Student 2 cannot see student 1's interview
            mockMvc.perform(get("/api/v1/interviews/" + interviewId)
                            .header("Authorization", bearer(stu2.getAccessToken())))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  RECRUITER INTERVIEW ACCESS TESTS
    // ──────────────────────────────────────

    @Nested
    class RecruiterAccessTests {

        @Test
        void recruiterSeesOwnCompanyInterviews() throws Exception {
            Object[] env = setupFullEnvironment("rec-acc");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            // Schedule interview
            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.MANAGERIAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);

            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/interviews/recruiter")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void recruiterCannotSeeAnotherCompanyInterviews() throws Exception {
            Object[] env = setupFullEnvironment("rec-idor");
            String applicationId = (String) env[3];

            // Schedule interview
            AuthResponse recAuth = (AuthResponse) env[0];
            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.TECHNICAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);
            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            // Register a different recruiter with a different company
            AuthResponse rec2 = register("rec2-ior@test.com", "Password123!", "Rec2", Role.RECRUITER);
            CreateCompanyRequest comp2 = new CreateCompanyRequest();
            comp2.setName("OtherCo");
            comp2.setIndustry("Finance");
            mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comp2)))
                    .andExpect(status().isCreated());

            // Rec2 sees 0 interviews
            mockMvc.perform(get("/api/v1/interviews/recruiter")
                            .header("Authorization", bearer(rec2.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    // ──────────────────────────────────────
    //  RESCHEDULE, CANCEL, COMPLETE TESTS
    // ──────────────────────────────────────

    @Nested
    class LifecycleTests {

        @Test
        void recruiterReschedulesInterview() throws Exception {
            Object[] env = setupFullEnvironment("resched");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            // Schedule
            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.TECHNICAL);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(60);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            // Reschedule
            RescheduleInterviewRequest reschedReq = new RescheduleInterviewRequest();
            reschedReq.setScheduledAt(OffsetDateTime.now().plusDays(7));
            reschedReq.setDuration(90);

            mockMvc.perform(patch("/api/v1/interviews/" + interviewId + "/reschedule")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reschedReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESCHEDULED"));
        }

        @Test
        void recruiterCancelsInterview() throws Exception {
            Object[] env = setupFullEnvironment("cancel");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.HR);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(30);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/cancel")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void recruiterCompletesInterview() throws Exception {
            Object[] env = setupFullEnvironment("complete");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.TECHNICAL);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(60);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void cannotCompleteCancelledInterview() throws Exception {
            Object[] env = setupFullEnvironment("no-complete-cancel");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.HR);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(30);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            // Cancel
            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/cancel")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk());

            // Cannot complete cancelled
            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────
    //  FEEDBACK TESTS
    // ──────────────────────────────────────

    @Nested
    class FeedbackTests {

        @Test
        void recruiterSubmitsFeedbackOnCompletedInterview() throws Exception {
            Object[] env = setupFullEnvironment("feedback");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            // Schedule + complete
            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.TECHNICAL);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(60);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk());

            // Submit feedback
            SubmitFeedbackRequest fbReq = new SubmitFeedbackRequest();
            fbReq.setFeedback("Strong candidate with good communication skills");
            fbReq.setNotes("Recommended for next round");

            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/feedback")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fbReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.feedback").value("Strong candidate with good communication skills"));
        }

        @Test
        void feedbackRejectedBeforeCompletion() throws Exception {
            Object[] env = setupFullEnvironment("fb-premature");
            AuthResponse recAuth = (AuthResponse) env[0];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.HR);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(45);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            SubmitFeedbackRequest fbReq = new SubmitFeedbackRequest();
            fbReq.setFeedback("Some feedback");

            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/feedback")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fbReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void studentCanReadFeedback() throws Exception {
            Object[] env = setupFullEnvironment("stu-fb");
            AuthResponse recAuth = (AuthResponse) env[0];
            AuthResponse stuAuth = (AuthResponse) env[1];
            String applicationId = (String) env[3];

            // Schedule + complete + feedback
            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.TECHNICAL);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(60);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk());

            SubmitFeedbackRequest fbReq = new SubmitFeedbackRequest();
            fbReq.setFeedback("Excellent work");
            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/feedback")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fbReq)))
                    .andExpect(status().isOk());

            // Student reads feedback
            mockMvc.perform(get("/api/v1/interviews/" + interviewId)
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.feedback").value("Excellent work"));
        }

        @Test
        void studentCannotModifyFeedback() throws Exception {
            Object[] env = setupFullEnvironment("stu-no-fb");
            AuthResponse recAuth = (AuthResponse) env[0];
            AuthResponse stuAuth = (AuthResponse) env[1];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
            schedReq.setApplicationId(UUID.fromString(applicationId));
            schedReq.setInterviewType(InterviewType.TECHNICAL);
            schedReq.setScheduledAt(OffsetDateTime.now().plusDays(3));
            schedReq.setDuration(60);
            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(schedReq)))
                    .andExpect(status().isCreated()).andReturn();
            String interviewId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();

            // Complete the interview first
            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/complete")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk());

            // Student tries to submit feedback → 403
            SubmitFeedbackRequest fbReq = new SubmitFeedbackRequest();
            fbReq.setFeedback("I rate myself highly");
            mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/feedback")
                            .header("Authorization", bearer(stuAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fbReq)))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  NOTIFICATION TESTS
    // ──────────────────────────────────────

    @Nested
    class NotificationTests {

        @Test
        void userSeesOwnNotifications() throws Exception {
            Object[] env = setupFullEnvironment("notif1");
            AuthResponse recAuth = (AuthResponse) env[0];

            // "New Application" notification goes to the recruiter
            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("New Application"));
        }

        @Test
        void userCannotSeeAnotherUsersNotifications() throws Exception {
            Object[] env = setupFullEnvironment("notif-idor");
            AuthResponse recAuth = (AuthResponse) env[0];

            // Register another recruiter
            AuthResponse rec2 = register("rec2-notif@test.com", "Password123!", "Rec2", Role.RECRUITER);

            // Rec2 sees 0 notifications (different company)
            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", bearer(rec2.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        void markNotificationRead() throws Exception {
            Object[] env = setupFullEnvironment("notif-read");
            AuthResponse recAuth = (AuthResponse) env[0];

            // Get the notification ("New Application" goes to recruiter)
            MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk()).andReturn();
            JsonNode notifList = objectMapper.readTree(result.getResponse().getContentAsString());
            String notifId = notifList.path("content").get(0).path("id").asText();

            mockMvc.perform(patch("/api/v1/notifications/" + notifId + "/read")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.read").value(true));
        }

        @Test
        void unreadCountCorrect() throws Exception {
            Object[] env = setupFullEnvironment("unread-count");
            AuthResponse recAuth = (AuthResponse) env[0];

            // "New Application" notification goes to recruiter
            mockMvc.perform(get("/api/v1/notifications/unread-count")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        void markAllAsRead() throws Exception {
            Object[] env = setupFullEnvironment("read-all");
            AuthResponse recAuth = (AuthResponse) env[0];

            // "New Application" notification goes to recruiter
            mockMvc.perform(patch("/api/v1/notifications/read-all")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updated").value(1));

            mockMvc.perform(get("/api/v1/notifications/unread-count")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));
        }

        @Test
        void interviewScheduleCreatesNotification() throws Exception {
            Object[] env = setupFullEnvironment("int-notif");
            AuthResponse recAuth = (AuthResponse) env[0];
            AuthResponse stuAuth = (AuthResponse) env[1];
            String applicationId = (String) env[3];

            ScheduleInterviewRequest req = new ScheduleInterviewRequest();
            req.setApplicationId(UUID.fromString(applicationId));
            req.setInterviewType(InterviewType.TECHNICAL);
            req.setScheduledAt(OffsetDateTime.now().plusDays(3));
            req.setDuration(60);

            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", bearer(recAuth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            // Student gets 1 notification: interview scheduled
            // (application notification goes to recruiter, not student)
            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", bearer(stuAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Interview Scheduled"));

            // Recruiter gets 1 notification: new application
            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", bearer(recAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("New Application"));
        }
    }
}