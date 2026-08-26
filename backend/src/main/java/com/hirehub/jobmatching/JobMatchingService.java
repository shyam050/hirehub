package com.hirehub.jobmatching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.ai.*;
import com.hirehub.ai.config.AiConfig;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.job.entity.Job;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.jobmatching.dto.JobMatchResponse;
import com.hirehub.jobmatching.dto.SkillGapResponse;
import com.hirehub.jobmatching.entity.JobMatch;
import com.hirehub.jobmatching.repository.JobMatchRepository;
import com.hirehub.resume.entity.Resume;
import com.hirehub.resume.repository.ResumeRepository;
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

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final JobMatchRepository matchRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AiResilienceService aiService;
    private final SkillMatchingService skillMatchingService;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;

    // Blending weights: 70% AI, 30% deterministic
    private static final double AI_WEIGHT = 0.70;
    private static final double DETERMINISTIC_WEIGHT = 0.30;

    /**
     * Calculate match for a student against a job.
     */
    @Transactional
    public JobMatchResponse calculateMatch(Authentication auth, UUID jobId, UUID resumeId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can calculate job matches");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        // Resolve resume
        Resume resume;
        if (resumeId != null) {
            resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId.toString()));
            if (!resume.getStudent().getId().equals(student.getId())) {
                throw new ForbiddenException("You can only use your own resumes");
            }
        } else {
            resume = resumeRepository.findDefaultByStudentId(student.getId())
                    .orElseThrow(() -> new IllegalStateException("No default resume found. Upload a resume first."));
        }

        // Check for fresh cached match
        OffsetDateTime cacheExpiry = OffsetDateTime.now().minusDays(aiConfig.getAnalysis().getCacheDays());
        var cached = matchRepository.findRecentMatch(student.getId(), job.getId(), resume.getId(), cacheExpiry);
        if (cached.isPresent()) {
            log.info("Returning cached match for student {} job {} resume {}", student.getId(), jobId, resume.getId());
            return toResponse(cached.get());
        }

        // Build student context
        String studentContext = buildStudentContext(student, resume);

        // Build job context
        String jobContext = buildJobContext(job);

        // Deterministic skill match
        SkillMatchingService.SkillMatchResult skillResult =
                skillMatchingService.calculateMatch(student.getSkills(), job.getSkills());

        // AI match
        JobMatchAnalysisResult aiResult;
        try {
            aiResult = aiService.analyzeJobMatch(studentContext, jobContext);
        } catch (AiServiceException e) {
            throw new IllegalStateException("AI job matching failed: " + e.getMessage());
        }

        // Blend scores
        long deterministicScore = skillResult.score();
        int finalScore = (int) Math.round(aiResult.getAiScore() * AI_WEIGHT + deterministicScore * DETERMINISTIC_WEIGHT);
        finalScore = Math.max(0, Math.min(100, finalScore));

        // Merge matched/missing skills from both sources
        List<String> allMatched = new ArrayList<>(new LinkedHashSet<>(skillResult.matchedSkills()));
        allMatched.addAll(aiResult.getMatchedSkills());
        allMatched = allMatched.stream().distinct().toList();

        List<String> allMissing = new ArrayList<>(new LinkedHashSet<>(skillResult.missingSkills()));
        allMissing.addAll(aiResult.getMissingSkills());
        allMissing = allMissing.stream().distinct().toList();

        // Save or update match
        Optional<JobMatch> existing = matchRepository.findByStudentIdAndJobIdAndResumeId(
                student.getId(), job.getId(), resume.getId());

        JobMatch match;
        if (existing.isPresent()) {
            match = existing.get();
            match.setMatchScore(finalScore);
            match.setMatchedSkills(toJson(allMatched));
            match.setMissingSkills(toJson(allMissing));
            match.setStrengths(toJson(aiResult.getStrengths()));
            match.setRecommendations(toJson(aiResult.getRecommendations()));
            match.setExplanation(aiResult.getExplanation());
        } else {
            match = JobMatch.builder()
                    .student(student)
                    .job(job)
                    .resume(resume)
                    .matchScore(finalScore)
                    .matchedSkills(toJson(allMatched))
                    .missingSkills(toJson(allMissing))
                    .strengths(toJson(aiResult.getStrengths()))
                    .recommendations(toJson(aiResult.getRecommendations()))
                    .explanation(aiResult.getExplanation())
                    .build();
        }

        match = matchRepository.save(match);
        log.info("Job match saved: {} score={}", match.getId(), finalScore);
        return toResponse(match);
    }

    // ── Query Methods ──

    @Transactional(readOnly = true)
    public JobMatchResponse getMatchForJob(Authentication auth, UUID jobId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can view job matches");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Resume resume = resumeRepository.findDefaultByStudentId(student.getId())
                .orElseThrow(() -> new IllegalStateException("No default resume found"));

        JobMatch match = matchRepository.findByStudentIdAndJobIdAndResumeId(
                student.getId(), jobId, resume.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No match found for this job"));

        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public Page<JobMatchResponse> getMyMatches(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can view their matches");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return matchRepository.findByStudentIdOrderByCreatedAtDesc(student.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<SkillGapResponse> getSkillGaps(Authentication auth) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can view skill gaps");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        List<JobMatch> matches = matchRepository.findByStudentIdOrderByMatchScoreDesc(student.getId());

        // Aggregate missing skills
        Map<String, Long> skillCounts = new LinkedHashMap<>();
        for (JobMatch match : matches) {
            List<String> missing = fromJson(match.getMissingSkills());
            for (String skill : missing) {
                skillCounts.merge(skill, 1L, Long::sum);
            }
        }

        return skillCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> SkillGapResponse.builder().skill(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobMatchResponse> getMatchesForResume(Authentication auth, UUID resumeId) {
        User user = findUserByEmail(auth.getName());
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId.toString()));

        // Verify ownership
        switch (user.getRole()) {
            case ADMIN -> { /* full access */ }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student profile"));
                if (!resume.getStudent().getId().equals(student.getId())) {
                    throw new ForbiddenException("You can only view matches for your own resumes");
                }
            }
            default -> throw new ForbiddenException("Access denied");
        }

        return matchRepository.findByResumeIdOrderByCreatedAtDesc(resumeId)
                .stream().map(this::toResponse).toList();
    }

    // ── Helpers ──

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String buildStudentContext(Student student, Resume resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("Skills: ").append(student.getSkills()).append("\n");
        sb.append("Education: ").append(student.getEducation()).append("\n");
        sb.append("Projects: ").append(student.getProjects()).append("\n");
        sb.append("University: ").append(student.getUniversity()).append("\n");
        sb.append("Degree: ").append(student.getDegree()).append("\n");
        sb.append("Field: ").append(student.getFieldOfStudy()).append("\n");
        sb.append("Graduation: ").append(student.getGraduationYear()).append("\n");
        if (resume.getExtractedText() != null && !resume.getExtractedText().isBlank()) {
            sb.append("Resume excerpt: ").append(resume.getExtractedText(), 0,
                    Math.min(resume.getExtractedText().length(), 3000));
        }
        return sb.toString();
    }

    private String buildJobContext(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(job.getTitle()).append("\n");
        sb.append("Description: ").append(job.getDescription()).append("\n");
        sb.append("Required Skills: ").append(job.getSkills()).append("\n");
        sb.append("Location: ").append(job.getLocation()).append("\n");
        sb.append("Job Type: ").append(job.getJobType()).append("\n");
        sb.append("Education Required: ").append(job.getEducationRequired()).append("\n");
        sb.append("Experience: ").append(job.getExperienceMin()).append("-").append(job.getExperienceMax()).append(" years\n");
        sb.append("Requirements: ").append(job.getRequirements()).append("\n");
        return sb.toString();
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

    private JobMatchResponse toResponse(JobMatch match) {
        return JobMatchResponse.builder()
                .id(match.getId())
                .jobId(match.getJob() != null ? match.getJob().getId() : null)
                .jobTitle(match.getJob() != null ? match.getJob().getTitle() : null)
                .companyName(match.getJob() != null && match.getJob().getCompany() != null ?
                        match.getJob().getCompany().getName() : null)
                .resumeId(match.getResume() != null ? match.getResume().getId() : null)
                .resumeFileName(match.getResume() != null ? match.getResume().getFileName() : null)
                .matchScore(match.getMatchScore())
                .matchedSkills(fromJson(match.getMatchedSkills()))
                .missingSkills(fromJson(match.getMissingSkills()))
                .strengths(fromJson(match.getStrengths()))
                .recommendations(fromJson(match.getRecommendations()))
                .explanation(match.getExplanation())
                .createdAt(match.getCreatedAt())
                .updatedAt(match.getUpdatedAt())
                .build();
    }
}
