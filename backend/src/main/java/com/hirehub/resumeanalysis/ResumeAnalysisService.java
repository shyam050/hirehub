package com.hirehub.resumeanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.ai.AiResilienceService;
import com.hirehub.ai.AiServiceException;
import com.hirehub.ai.ResumeAnalysisResult;
import com.hirehub.ai.config.AiConfig;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.resume.entity.Resume;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.resume.storage.FileStorageService;
import com.hirehub.resume.text.ResumeTextExtractionService;
import com.hirehub.resumeanalysis.dto.ResumeAnalysisResponse;
import com.hirehub.resumeanalysis.entity.ResumeAnalysis;
import com.hirehub.resumeanalysis.repository.ResumeAnalysisRepository;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    private final ResumeAnalysisRepository analysisRepository;
    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AiResilienceService aiService;
    private final FileStorageService fileStorageService;
    private final ResumeTextExtractionService textExtractionService;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;

    /**
     * Analyze a resume. Checks for cached result before calling AI.
     */
    @Transactional
    public ResumeAnalysisResponse analyzeResume(Authentication auth, UUID resumeId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can analyze resumes");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId.toString()));

        // Verify ownership
        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only analyze your own resumes");
        }

        // Check for recent cached analysis
        OffsetDateTime cacheExpiry = OffsetDateTime.now().minusDays(aiConfig.getAnalysis().getCacheDays());
        var cachedAnalysis = analysisRepository.findRecentByResumeId(resumeId, cacheExpiry);
        if (cachedAnalysis.isPresent()) {
            log.info("Returning cached analysis for resume {}", resumeId);
            return toResponse(cachedAnalysis.get());
        }

        // Extract text from PDF
        String extractedText;
        if (resume.getExtractedText() != null && !resume.getExtractedText().isBlank()) {
            extractedText = resume.getExtractedText();
        } else {
            try (InputStream is = fileStorageService.retrieve(resume.getStorageId())) {
                extractedText = textExtractionService.extractText(is);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to extract text from resume: " + e.getMessage());
            }
            // Store extracted text on the resume
            resume.setExtractedText(extractedText);
            resumeRepository.save(resume);
        }

        // Call AI service
        ResumeAnalysisResult aiResult;
        try {
            aiResult = aiService.analyzeResume(extractedText);
        } catch (AiServiceException e) {
            throw new IllegalStateException("AI analysis failed: " + e.getMessage());
        }

        // Save analysis
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .student(student)
                .resume(resume)
                .overallScore(aiResult.getOverallScore())
                .extractedSkills(toJson(aiResult.getExtractedSkills()))
                .extractedEducation(toJson(aiResult.getExtractedEducation()))
                .extractedProjects(toJson(aiResult.getExtractedProjects()))
                .extractedExperience(toJson(aiResult.getExtractedExperience()))
                .extractedCertifications(toJson(aiResult.getExtractedCertifications()))
                .extractedAchievements(toJson(aiResult.getExtractedAchievements()))
                .strengths(toJson(aiResult.getStrengths()))
                .weaknesses(toJson(aiResult.getWeaknesses()))
                .missingSkills(toJson(aiResult.getMissingSkills()))
                .recommendations(toJson(aiResult.getRecommendations()))
                .build();

        analysis = analysisRepository.save(analysis);
        log.info("Resume analysis saved: {} for resume {}", analysis.getId(), resumeId);

        return toResponse(analysis);
    }

    /**
     * Get all analyses for a specific resume.
     */
    @Transactional(readOnly = true)
    public List<ResumeAnalysisResponse> getAnalysesForResume(Authentication auth, UUID resumeId) {
        User user = findUserByEmail(auth.getName());
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId.toString()));

        authorizeResumeAccess(user, resume);

        return analysisRepository.findByResumeIdOrderByCreatedAtDesc(resumeId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Get the latest analysis for a specific resume.
     */
    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getLatestAnalysis(Authentication auth, UUID resumeId) {
        User user = findUserByEmail(auth.getName());
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId.toString()));

        authorizeResumeAccess(user, resume);

        ResumeAnalysis analysis = analysisRepository.findFirstByResumeIdOrderByCreatedAtDesc(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis found for this resume"));

        return toResponse(analysis);
    }

    /**
     * Get all analyses for the current student across all resumes.
     */
    @Transactional(readOnly = true)
    public Page<ResumeAnalysisResponse> getMyAnalyses(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can view their analyses");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return analysisRepository.findByStudentIdOrderByCreatedAtDesc(student.getId(), pageable)
                .map(this::toResponse);
    }

    // ── Authorization ──

    private void authorizeResumeAccess(User user, Resume resume) {
        switch (user.getRole()) {
            case ADMIN -> { /* full access */ }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student profile"));
                if (!resume.getStudent().getId().equals(student.getId())) {
                    throw new ForbiddenException("You can only view analyses for your own resumes");
                }
            }
            default -> throw new ForbiddenException("Access denied");
        }
    }

    // ── Helpers ──

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private ResumeAnalysisResponse toResponse(ResumeAnalysis analysis) {
        return ResumeAnalysisResponse.builder()
                .id(analysis.getId())
                .resumeId(analysis.getResume() != null ? analysis.getResume().getId() : null)
                .resumeFileName(analysis.getResume() != null ? analysis.getResume().getFileName() : null)
                .overallScore(analysis.getOverallScore())
                .extractedSkills(fromJson(analysis.getExtractedSkills()))
                .extractedEducation(fromJson(analysis.getExtractedEducation()))
                .extractedProjects(fromJson(analysis.getExtractedProjects()))
                .extractedExperience(fromJson(analysis.getExtractedExperience()))
                .extractedCertifications(fromJson(analysis.getExtractedCertifications()))
                .extractedAchievements(fromJson(analysis.getExtractedAchievements()))
                .strengths(fromJson(analysis.getStrengths()))
                .weaknesses(fromJson(analysis.getWeaknesses()))
                .missingSkills(fromJson(analysis.getMissingSkills()))
                .recommendations(fromJson(analysis.getRecommendations()))
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }
}
