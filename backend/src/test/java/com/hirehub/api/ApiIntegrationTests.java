package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.LoginRequest;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.entity.Company;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.aiinterview.repository.AiInterviewQuestionRepository;
import com.hirehub.aiinterview.repository.AiInterviewRepository;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.dto.UpdateStudentProfileRequest;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.dto.UpdateUserRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AiInterviewQuestionRepository aiInterviewQuestionRepository;
    @Autowired private AiInterviewRepository aiInterviewRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobMatchRepository matchRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private ResumeAnalysisRepository analysisRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        aiInterviewQuestionRepository.deleteAll();
        aiInterviewRepository.deleteAll();
        matchRepository.deleteAll();
        analysisRepository.deleteAll();
        notificationRepository.deleteAll();
        applicationRepository.deleteAll();
        interviewRepository.deleteAll();
        resumeRepository.deleteAll();
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
        req.setEmail(email);
        req.setPassword(password);
        req.setName(name);
        req.setRole(role);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractAuth(result);
    }

    private AuthResponse login(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
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

    // ──────────────────────────────────────
    //  USER TESTS
    // ──────────────────────────────────────

    @Nested
    class UserTests {

        @Test
        void getMe_returnsUserProfile() throws Exception {
            AuthResponse auth = register("user@test.com", "Password123!", "John Doe", Role.STUDENT);

            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.name").value("John Doe"))
                    .andExpect(jsonPath("$.data.role").value("STUDENT"));
        }

        @Test
        void updateMe_updatesName() throws Exception {
            AuthResponse auth = register("upd@test.com", "Password123!", "Old Name", Role.STUDENT);

            UpdateUserRequest req = new UpdateUserRequest();
            req.setName("New Name");

            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("New Name"));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void normalUser_cannotAccessAdminEndpoint() throws Exception {
            AuthResponse auth = register("no-admin@test.com", "Password123!", "User", Role.STUDENT);
            mockMvc.perform(get("/api/v1/users/" + auth.getUser().getId())
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void roleCannotBeChangedViaProfileApi() throws Exception {
            AuthResponse auth = register("role@test.com", "Password123!", "Test", Role.STUDENT);
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Changed\",\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(jsonPath("$.data.role").value("STUDENT"));
        }
    }

    // ──────────────────────────────────────
    //  STUDENT TESTS
    // ──────────────────────────────────────

    @Nested
    class StudentTests {

        @Test
        void getStudentProfile() throws Exception {
            AuthResponse auth = register("stu1@test.com", "Password123!", "Jane", Role.STUDENT);
            mockMvc.perform(get("/api/v1/students/me")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("stu1@test.com"));
        }

        @Test
        void updateStudentProfile() throws Exception {
            AuthResponse auth = register("stu2@test.com", "Password123!", "Student", Role.STUDENT);

            UpdateStudentProfileRequest req = new UpdateStudentProfileRequest();
            req.setUniversity("MIT");
            req.setDegree("BSc CS");

            mockMvc.perform(put("/api/v1/students/me")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.university").value("MIT"))
                    .andExpect(jsonPath("$.data.degree").value("BSc CS"));
        }

        @Test
        void studentCannotAccessOtherStudent() throws Exception {
            AuthResponse s1 = register("s1@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2@test.com", "Password123!", "S2", Role.STUDENT);

            // Get s2's student ID from their profile
            MvcResult s2Profile = mockMvc.perform(get("/api/v1/students/me")
                            .header("Authorization", bearer(s2.getAccessToken())))
                    .andReturn();
            JsonNode s2Data = objectMapper.readTree(s2Profile.getResponse().getContentAsString()).path("data");
            String s2StudentId = s2Data.path("id").asText();

            mockMvc.perform(get("/api/v1/students/" + s2StudentId)
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotAccessStudentProfile() throws Exception {
            AuthResponse stu = register("rs@test.com", "Password123!", "Stu", Role.STUDENT);
            AuthResponse rec = register("rs-r@test.com", "Password123!", "Rec", Role.RECRUITER);

            MvcResult stuProfile = mockMvc.perform(get("/api/v1/students/me")
                            .header("Authorization", bearer(stu.getAccessToken())))
                    .andReturn();
            String stuId = objectMapper.readTree(stuProfile.getResponse().getContentAsString()).path("data").path("id").asText();

            mockMvc.perform(get("/api/v1/students/" + stuId)
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotUseStudentMe() throws Exception {
            AuthResponse rec = register("rme@test.com", "Password123!", "Rec", Role.RECRUITER);
            mockMvc.perform(get("/api/v1/students/me")
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  RECRUITER TESTS
    // ──────────────────────────────────────

    @Nested
    class RecruiterTests {

        @Test
        void getRecruiterProfile() throws Exception {
            AuthResponse auth = register("rec1@test.com", "Password123!", "Rec", Role.RECRUITER);
            mockMvc.perform(get("/api/v1/recruiters/me")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("rec1@test.com"));
        }

        @Test
        void updateRecruiterProfile() throws Exception {
            AuthResponse auth = register("rec2@test.com", "Password123!", "Rec", Role.RECRUITER);

            com.hirehub.recruiter.dto.UpdateRecruiterProfileRequest req =
                    new com.hirehub.recruiter.dto.UpdateRecruiterProfileRequest();
            req.setJobTitle("Senior Recruiter");

            mockMvc.perform(put("/api/v1/recruiters/me")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.jobTitle").value("Senior Recruiter"));
        }

        @Test
        void studentCannotAccessRecruiterMe() throws Exception {
            AuthResponse stu = register("sr@test.com", "Password123!", "Stu", Role.STUDENT);
            mockMvc.perform(get("/api/v1/recruiters/me")
                            .header("Authorization", bearer(stu.getAccessToken())))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  COMPANY TESTS
    // ──────────────────────────────────────

    @Nested
    class CompanyTests {

        @Test
        void recruiterCreatesCompany() throws Exception {
            AuthResponse auth = register("cc@test.com", "Password123!", "Owner", Role.RECRUITER);

            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("TechCorp");
            req.setIndustry("Technology");

            mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("TechCorp"))
                    .andExpect(jsonPath("$.data.approved").value(false));
        }

        @Test
        void recruiterUpdatesOwnCompany() throws Exception {
            AuthResponse auth = register("uc@test.com", "Password123!", "Owner", Role.RECRUITER);

            CreateCompanyRequest createReq = new CreateCompanyRequest();
            createReq.setName("MyCo");

            MvcResult result = mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CompanyResponse company = extractCompany(result);

            com.hirehub.company.dto.UpdateCompanyRequest updateReq =
                    new com.hirehub.company.dto.UpdateCompanyRequest();
            updateReq.setName("MyCo Updated");

            mockMvc.perform(put("/api/v1/companies/" + company.getId())
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("MyCo Updated"));
        }

        @Test
        void studentCannotCreateCompany() throws Exception {
            AuthResponse auth = register("sc@test.com", "Password123!", "Stu", Role.STUDENT);
            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("StuCo");
            mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotApproveOwnCompany() throws Exception {
            AuthResponse auth = register("rca@test.com", "Password123!", "Rec", Role.RECRUITER);
            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("ApproveTest");

            MvcResult result = mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CompanyResponse company = extractCompany(result);

            mockMvc.perform(post("/api/v1/companies/" + company.getId() + "/approve")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminCanApproveCompany() throws Exception {
            AuthResponse rec = register("rac@test.com", "Password123!", "Rec", Role.RECRUITER);
            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("ApproveMe");

            MvcResult result = mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(rec.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CompanyResponse company = extractCompany(result);

            User admin = createAdmin("admin-approve@test.com");
            AuthResponse adminAuth = login("admin-approve@test.com", "Password123!");

            mockMvc.perform(post("/api/v1/companies/" + company.getId() + "/approve")
                            .header("Authorization", bearer(adminAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.approved").value(true));
        }

        @Test
        void adminCanRejectCompany() throws Exception {
            AuthResponse rec = register("rrc@test.com", "Password123!", "Rec", Role.RECRUITER);
            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("RejectMe");

            MvcResult result = mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(rec.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CompanyResponse company = extractCompany(result);

            User admin = createAdmin("admin-reject@test.com");
            AuthResponse adminAuth = login("admin-reject@test.com", "Password123!");

            mockMvc.perform(post("/api/v1/companies/" + company.getId() + "/reject")
                            .header("Authorization", bearer(adminAuth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.approved").value(false));
        }

        @Test
        void recruiterCannotUpdateOtherCompany() throws Exception {
            AuthResponse rec1 = register("r1o@test.com", "Password123!", "Rec1", Role.RECRUITER);
            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("Rec1Co");

            MvcResult result = mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(rec1.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CompanyResponse company = extractCompany(result);

            AuthResponse rec2 = register("r2o@test.com", "Password123!", "Rec2", Role.RECRUITER);
            com.hirehub.company.dto.UpdateCompanyRequest updateReq =
                    new com.hirehub.company.dto.UpdateCompanyRequest();
            updateReq.setName("Hijacked");

            mockMvc.perform(put("/api/v1/companies/" + company.getId())
                            .header("Authorization", bearer(rec2.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getCompanyIsPublic() throws Exception {
            AuthResponse auth = register("pc@test.com", "Password123!", "Owner", Role.RECRUITER);
            CreateCompanyRequest req = new CreateCompanyRequest();
            req.setName("PublicCo");

            MvcResult result = mockMvc.perform(post("/api/v1/companies")
                            .header("Authorization", bearer(auth.getAccessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CompanyResponse company = extractCompany(result);

            mockMvc.perform(get("/api/v1/companies/" + company.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("PublicCo"));
        }
    }
}
