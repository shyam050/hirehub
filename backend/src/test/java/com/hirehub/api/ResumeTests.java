package com.hirehub.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.auth.dto.AuthResponse;
import com.hirehub.auth.dto.RegisterRequest;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.student.entity.Student;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumeTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private JobMatchRepository matchRepository;
    @Autowired private ResumeAnalysisRepository analysisRepository;
    
    
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        matchRepository.deleteAll();
        analysisRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        resumeRepository.deleteAll();
        applicationRepository.deleteAll();
        interviewRepository.deleteAll();
        jobRepository.deleteAll();
        notificationRepository.deleteAll();
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

    private MockMultipartFile pdfFile(String name, int sizeBytes) {
        byte[] content = new byte[sizeBytes];
        // Fill with minimal PDF content
        String pdfHeader = "%PDF-1.4\n";
        byte[] header = pdfHeader.getBytes();
        System.arraycopy(header, 0, content, 0, Math.min(header.length, sizeBytes));
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    private MockMultipartFile nonPdfFile() {
        return new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
    }

    private MockMultipartFile emptyPdfFile() {
        return new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    }

    private MvcResult uploadResume(String token, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/v1/resumes")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andReturn();
    }

    // ──────────────────────────────────────
    //  UPLOAD TESTS
    // ──────────────────────────────────────

    @Nested
    class UploadTests {

        @Test
        void studentUploadsPdfSuccessfully() throws Exception {
            AuthResponse auth = register("upload@test.com", "Password123!", "Student", Role.STUDENT);

            mockMvc.perform(multipart("/api/v1/resumes")
                            .file(pdfFile("resume.pdf", 1024))
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileName").value("resume.pdf"))
                    .andExpect(jsonPath("$.fileSize").value(1024))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.isDefault").value(true));
        }

        @Test
        void firstResumeIsDefault() throws Exception {
            AuthResponse auth = register("first-default@test.com", "Password123!", "Student", Role.STUDENT);

            mockMvc.perform(multipart("/api/v1/resumes")
                            .file(pdfFile("first.pdf", 500))
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isDefault").value(true));
        }

        @Test
        void nonPdfFileRejected() throws Exception {
            AuthResponse auth = register("nonpdf@test.com", "Password123!", "Student", Role.STUDENT);

            mockMvc.perform(multipart("/api/v1/resumes")
                            .file(nonPdfFile())
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void emptyFileRejected() throws Exception {
            AuthResponse auth = register("empty@test.com", "Password123!", "Student", Role.STUDENT);

            mockMvc.perform(multipart("/api/v1/resumes")
                            .file(emptyPdfFile())
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void recruiterCannotUploadResume() throws Exception {
            AuthResponse auth = register("rec-upload@test.com", "Password123!", "Recruiter", Role.RECRUITER);

            mockMvc.perform(multipart("/api/v1/resumes")
                            .file(pdfFile("resume.pdf", 500))
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedCannotUpload() throws Exception {
            mockMvc.perform(multipart("/api/v1/resumes")
                            .file(pdfFile("resume.pdf", 500)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────
    //  LIST TESTS
    // ──────────────────────────────────────

    @Nested
    class ListTests {

        @Test
        void studentListsOwnResumes() throws Exception {
            AuthResponse auth = register("list@test.com", "Password123!", "Student", Role.STUDENT);
            uploadResume(auth.getAccessToken(), pdfFile("resume1.pdf", 1024));
            uploadResume(auth.getAccessToken(), pdfFile("resume2.pdf", 2048));

            mockMvc.perform(get("/api/v1/resumes")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void unauthenticatedCannotList() throws Exception {
            mockMvc.perform(get("/api/v1/resumes"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────
    //  GET SINGLE RESUME TESTS
    // ──────────────────────────────────────

    @Nested
    class GetResumeTests {

        @Test
        void studentGetsOwnResume() throws Exception {
            AuthResponse auth = register("get@test.com", "Password123!", "Student", Role.STUDENT);
            MvcResult upload = uploadResume(auth.getAccessToken(), pdfFile("myresume.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileName").value("myresume.pdf"));
        }

        @Test
        void studentCannotGetOtherStudentResume() throws Exception {
            AuthResponse s1 = register("s1get@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2get@test.com", "Password123!", "S2", Role.STUDENT);

            MvcResult upload = uploadResume(s2.getAccessToken(), pdfFile("s2resume.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void recruiterCannotGetStudentResume() throws Exception {
            AuthResponse stu = register("stu-resume@test.com", "Password123!", "Student", Role.STUDENT);
            AuthResponse rec = register("rec-resume@test.com", "Password123!", "Recruiter", Role.RECRUITER);

            MvcResult upload = uploadResume(stu.getAccessToken(), pdfFile("student.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(rec.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void nonexistentResumeReturns404() throws Exception {
            AuthResponse auth = register("404get@test.com", "Password123!", "Student", Role.STUDENT);
            String fakeId = UUID.randomUUID().toString();

            mockMvc.perform(get("/api/v1/resumes/" + fakeId)
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isNotFound());
        }
    }

    // ──────────────────────────────────────
    //  DOWNLOAD TESTS
    // ──────────────────────────────────────

    @Nested
    class DownloadTests {

        @Test
        void studentDownloadsOwnResume() throws Exception {
            AuthResponse auth = register("dl@test.com", "Password123!", "Student", Role.STUDENT);
            MvcResult upload = uploadResume(auth.getAccessToken(), pdfFile("download.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/download")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("download.pdf")));
        }

        @Test
        void studentCannotDownloadOtherStudentResume() throws Exception {
            AuthResponse s1 = register("s1dl@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2dl@test.com", "Password123!", "S2", Role.STUDENT);

            MvcResult upload = uploadResume(s2.getAccessToken(), pdfFile("private.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/download")
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedCannotDownload() throws Exception {
            AuthResponse auth = register("dlna@test.com", "Password123!", "Student", Role.STUDENT);
            MvcResult upload = uploadResume(auth.getAccessToken(), pdfFile("sec.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/download"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────
    //  DELETE TESTS
    // ──────────────────────────────────────

    @Nested
    class DeleteTests {

        @Test
        void studentDeletesOwnResume() throws Exception {
            AuthResponse auth = register("del@test.com", "Password123!", "Student", Role.STUDENT);
            MvcResult upload = uploadResume(auth.getAccessToken(), pdfFile("delete.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(delete("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isNotFound());
        }

        @Test
        void studentCannotDeleteOtherStudentResume() throws Exception {
            AuthResponse s1 = register("s1del@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2del@test.com", "Password123!", "S2", Role.STUDENT);

            MvcResult upload = uploadResume(s2.getAccessToken(), pdfFile("s2.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(delete("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deletingDefaultSetsNewDefault() throws Exception {
            AuthResponse auth = register("del-default@test.com", "Password123!", "Student", Role.STUDENT);

            // Upload two resumes
            MvcResult upload1 = uploadResume(auth.getAccessToken(), pdfFile("first.pdf", 1024));
            String id1 = objectMapper.readTree(upload1.getResponse().getContentAsString())
                    .path("id").asText();

            uploadResume(auth.getAccessToken(), pdfFile("second.pdf", 2048));

            // Delete the first (default)
            mockMvc.perform(delete("/api/v1/resumes/" + id1)
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isNoContent());

            // Second resume should now be default
            mockMvc.perform(get("/api/v1/resumes")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].isDefault").value(true));
        }
    }

    // ──────────────────────────────────────
    //  DEFAULT RESUME TESTS
    // ──────────────────────────────────────

    @Nested
    class DefaultResumeTests {

        @Test
        void studentSetsDefaultResume() throws Exception {
            AuthResponse auth = register("def@test.com", "Password123!", "Student", Role.STUDENT);

            MvcResult upload1 = uploadResume(auth.getAccessToken(), pdfFile("first.pdf", 1024));
            String id1 = objectMapper.readTree(upload1.getResponse().getContentAsString())
                    .path("id").asText();

            MvcResult upload2 = uploadResume(auth.getAccessToken(), pdfFile("second.pdf", 2048));
            String id2 = objectMapper.readTree(upload2.getResponse().getContentAsString())
                    .path("id").asText();

            // Set second as default
            mockMvc.perform(post("/api/v1/resumes/" + id2 + "/default")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isDefault").value(true));

            // Verify: list all resumes, exactly one has isDefault=true
            MvcResult listResult = mockMvc.perform(get("/api/v1/resumes")
                            .header("Authorization", bearer(auth.getAccessToken())))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode resumes = objectMapper.readTree(listResult.getResponse().getContentAsString());
            long defaultCount = 0;
            for (JsonNode r : resumes) {
                if (r.path("isDefault").asBoolean()) defaultCount++;
            }
            org.junit.jupiter.api.Assertions.assertEquals(1, defaultCount, "Exactly one resume should be default");
        }

        @Test
        void studentCannotSetOtherStudentDefault() throws Exception {
            AuthResponse s1 = register("s1def@test.com", "Password123!", "S1", Role.STUDENT);
            AuthResponse s2 = register("s2def@test.com", "Password123!", "S2", Role.STUDENT);

            MvcResult upload = uploadResume(s2.getAccessToken(), pdfFile("s2.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/default")
                            .header("Authorization", bearer(s1.getAccessToken())))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────
    //  ADMIN TESTS
    // ──────────────────────────────────────

    @Nested
    class AdminTests {

        @Test
        void adminCanViewStudentResume() throws Exception {
            AuthResponse stu = register("stu-admin@test.com", "Password123!", "Student", Role.STUDENT);
            MvcResult upload = uploadResume(stu.getAccessToken(), pdfFile("resume.pdf", 1024));
            String resumeId = objectMapper.readTree(upload.getResponse().getContentAsString())
                    .path("id").asText();

            // Create admin directly and login
            User admin = createAdmin("admin-resume@test.com");
            com.hirehub.auth.dto.LoginRequest loginReq = new com.hirehub.auth.dto.LoginRequest();
            loginReq.setEmail("admin-resume@test.com");
            loginReq.setPassword("Password123!");

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse adminAuthResponse = extractAuth(loginResult);

            mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                            .header("Authorization", bearer(adminAuthResponse.getAccessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileName").value("resume.pdf"));
        }


    }
}
