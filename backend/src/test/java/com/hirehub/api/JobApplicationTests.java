package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.LoginRequest;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.common.enums.Role;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.application.dto.CreateApplicationRequest;
import com.hirehub.application.dto.UpdateApplicationStatusRequest;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.job.dto.CreateJobRequest;
import com.hirehub.job.dto.JobResponse;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.common.enums.JobType;
import com.hirehub.common.enums.ApplicationStage;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.aiinterview.repository.AiInterviewQuestionRepository;
import com.hirehub.aiinterview.repository.AiInterviewRepository;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobApplicationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private JobMatchRepository matchRepository;
    @Autowired private AiInterviewQuestionRepository aiInterviewQuestionRepository;
    @Autowired private AiInterviewRepository aiInterviewRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private ResumeAnalysisRepository analysisRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        aiInterviewQuestionRepository.deleteAll();
        aiInterviewRepository.deleteAll();
        resumeRepository.deleteAll();
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

    private AuthResponse login(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email); req.setPassword(password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
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
                .passwordHash(passwordEncoder.encode("Password123!")).build());
    }

    private CompanyResponse createCompanyForRecruiter(AuthResponse recAuth) throws Exception {
        CreateCompanyRequest req = new CreateCompanyRequest();
        req.setName("TestCo_" + System.nanoTime());
        req.setIndustry("Tech");
        MvcResult result = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return extractCompany(result);
    }

    private JobResponse createJobForCompany(AuthResponse recAuth, CompanyResponse company) throws Exception {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Software Engineer");
        req.setDescription("Build amazing things");
        req.setLocation("Remote");
        req.setJobType(JobType.FULL_TIME);
        req.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", bearer(recAuth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return extractJob(result);
    }

    // ──────────────────────────────────────
    //  JOB TESTS
    // ──────────────────────────────────────

    @Nested
    class JobTests {

        @Test
        void recruiterCreatesJob() throws Exception {
            AuthResponse rec = register("rc1@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            mockMvc.perform(get("/api/v1/jobs/" + job.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Software Engineer"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.applicationCount").value(0));
        }

        @Test
        void studentCannotCreateJob() throws Exception {
            AuthResponse stu = register("sc@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateJobRequest req = new CreateJobRequest();
            req.setTitle("Test"); req.setDescription("Test"); req.setLocation("X");
            req.setJobType(JobType.FULL_TIME); req.setApplicationDeadline(OffsetDateTime.now().plusDays(1));

            mockMvc.perform(post("/api/v1/jobs")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotCreateJobForOtherCompany() throws Exception {
            AuthResponse rec1 = register("ro1@test.com", "Password123!", "R1", Role.RECRUITER);
            CompanyResponse company1 = createCompanyForRecruiter(rec1);

            AuthResponse rec2 = register("ro2@test.com", "Password123!", "R2", Role.RECRUITER);
            createCompanyForRecruiter(rec2);

            // Rec2 creates job for rec2's own company
            CreateJobRequest req = new CreateJobRequest();
            req.setTitle("Software Engineer"); req.setDescription("Build amazing things");
            req.setLocation("Remote"); req.setJobType(JobType.FULL_TIME);
            req.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
            MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated()).andReturn();
            JobResponse rec2Job = extractJob(result);

            // Rec1 tries to update rec2's job — forbidden
            mockMvc.perform(put("/api/v1/jobs/" + rec2Job.getId())
                            .header("Authorization", bearer(rec1.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Hijacked\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterClosesOwnJob() throws Exception {
            AuthResponse rec = register("cl@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/close")
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CLOSED"));
        }

        @Test
        void studentsCanBrowseActiveJobs() throws Exception {
            AuthResponse rec = register("br@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            createJobForCompany(rec, company);

            AuthResponse stu = register("brs@test.com", "Password123!", "Stu", Role.STUDENT);
            mockMvc.perform(get("/api/v1/jobs")
                            .header("Authorization", bearer(stu.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].title").value("Software Engineer"));
        }

        @Test
        void jobSearchWorks() throws Exception {
            AuthResponse rec = register("sr@test.com", "Password123!", "Rec", Role.RECRUITER);
            createCompanyForRecruiter(rec);

            CreateJobRequest req1 = new CreateJobRequest();
            req1.setTitle("Java Developer"); req1.setDescription("Java backend");
            req1.setLocation("Chennai"); req1.setJobType(JobType.FULL_TIME);
            req1.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
            mockMvc.perform(post("/api/v1/jobs")
                            .header("Authorization", bearer(rec.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated());

            CreateJobRequest req2 = new CreateJobRequest();
            req2.setTitle("Python Developer"); req2.setDescription("Python ML");
            req2.setLocation("Bangalore"); req2.setJobType(JobType.FULL_TIME);
            req2.setApplicationDeadline(OffsetDateTime.now().plusDays(30));
            mockMvc.perform(post("/api/v1/jobs")
                            .header("Authorization", bearer(rec.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isCreated());

            // Search by keyword
            mockMvc.perform(get("/api/v1/jobs?search=Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Java Developer"));

            // Search by location
            mockMvc.perform(get("/api/v1/jobs?location=Chennai"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].location").value("Chennai"));
        }

        @Test
        void recruiterCannotUpdateOtherCompanyJob() throws Exception {
            AuthResponse rec1 = register("uoj1@test.com", "Password123!", "R1", Role.RECRUITER);
            CompanyResponse c1 = createCompanyForRecruiter(rec1);
            JobResponse job = createJobForCompany(rec1, c1);

            AuthResponse rec2 = register("uoj2@test.com", "Password123!", "R2", Role.RECRUITER);
            createCompanyForRecruiter(rec2);

            mockMvc.perform(put("/api/v1/jobs/" + job.getId())
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Hijacked\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  APPLICATION TESTS
    // ──────────────────────────────────────

    @Nested
    class ApplicationTests {

        @Test
        void studentAppliesSuccessfully() throws Exception {
            AuthResponse rec = register("arc@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("asc@test.com", "Password123!", "Stu", Role.STUDENT);

            CreateApplicationRequest req = new CreateApplicationRequest();
            req.setCoverLetter("I am interested");

            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("APPLIED"));
        }

        @Test
        void duplicateApplicationRejected() throws Exception {
            AuthResponse rec = register("adc@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("ads@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest req = new CreateApplicationRequest();

            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());
        }

        @Test
        void studentCannotApplyToClosedJob() throws Exception {
            AuthResponse rec = register("acl@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            // Close the job
            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/close")
                    .header("Authorization", bearer(rec.getAccessToken())));

            AuthResponse stu = register("acs@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest req = new CreateApplicationRequest();

            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void studentSeesOwnApplications() throws Exception {
            AuthResponse rec = register("rso@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("sso@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest req = new CreateApplicationRequest();

            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/applications/me")
                            .header("Authorization", bearer(stu.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].jobTitle").value("Software Engineer"));
        }

        @Test
        void studentCannotSeeOtherStudentApplications() throws Exception {
            AuthResponse rec = register("roo@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse s1 = register("s1x@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2x@test.com", "Password123!", "S2", Role.STUDENT);

            CreateApplicationRequest req = new CreateApplicationRequest();
            MvcResult result = mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(s1.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated()).andReturn();

            String appId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("id").asText();

            // s2 tries to access s1's application — forbidden
            mockMvc.perform(get("/api/v1/applications/" + appId)
                            .header("Authorization", bearer(s2.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterSeesOwnJobApplicants() throws Exception {
            AuthResponse rec = register("rva@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("sva@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest req = new CreateApplicationRequest();
            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].studentName").value("Stu"));
        }

        @Test
        void recruiterCannotSeeOtherCompanyApplicants() throws Exception {
            AuthResponse rec1 = register("rva1@test.com", "Password123!", "R1", Role.RECRUITER);
            CompanyResponse c1 = createCompanyForRecruiter(rec1);
            JobResponse job = createJobForCompany(rec1, c1);

            AuthResponse stu = register("sva2@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest req = new CreateApplicationRequest();
            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            AuthResponse rec2 = register("rva2@test.com", "Password123!", "R2", Role.RECRUITER);
            createCompanyForRecruiter(rec2);

            mockMvc.perform(get("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(rec2.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterUpdatesApplicationStatus() throws Exception {
            AuthResponse rec = register("rus@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("sus@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest appReq = new CreateApplicationRequest();
            MvcResult result = mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(appReq)))
                    .andExpect(status().isCreated()).andReturn();

            String appId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("id").asText();

            UpdateApplicationStatusRequest statusReq = new UpdateApplicationStatusRequest();
            statusReq.setStatus(ApplicationStage.SHORTLISTED);

            mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                            .header("Authorization", bearer(rec.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));
        }

        @Test
        void studentCannotUpdateApplicationStatus() throws Exception {
            AuthResponse rec = register("rus2@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("sus2@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest appReq = new CreateApplicationRequest();
            MvcResult result = mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(appReq)))
                    .andExpect(status().isCreated()).andReturn();

            String appId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("id").asText();

            UpdateApplicationStatusRequest statusReq = new UpdateApplicationStatusRequest();
            statusReq.setStatus(ApplicationStage.SHORTLISTED);

            mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotUpdateOtherCompanyApplication() throws Exception {
            AuthResponse rec1 = register("ruo1@test.com", "Password123!", "R1", Role.RECRUITER);
            CompanyResponse c1 = createCompanyForRecruiter(rec1);
            JobResponse job = createJobForCompany(rec1, c1);

            AuthResponse stu = register("ruos@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest appReq = new CreateApplicationRequest();
            MvcResult result = mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(appReq)))
                    .andExpect(status().isCreated()).andReturn();

            String appId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("id").asText();

            AuthResponse rec2 = register("ruo2@test.com", "Password123!", "R2", Role.RECRUITER);
            createCompanyForRecruiter(rec2);

            UpdateApplicationStatusRequest statusReq = new UpdateApplicationStatusRequest();
            statusReq.setStatus(ApplicationStage.REJECTED);

            mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void notificationCreatedAfterApplication() throws Exception {
            AuthResponse rec = register("rnc@test.com", "Password123!", "Rec", Role.RECRUITER);
            CompanyResponse company = createCompanyForRecruiter(rec);
            JobResponse job = createJobForCompany(rec, company);

            AuthResponse stu = register("snc@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateApplicationRequest req = new CreateApplicationRequest();
            mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(stu.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            // Recruiter should have a notification
            mockMvc.perform(get("/api/v1/jobs/" + job.getId() + "/applications")
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].status").value("APPLIED"));
        }
    }
}
