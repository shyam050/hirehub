package com.hirehub.aiinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.ai.AiService;
import com.hirehub.ai.*;
import com.hirehub.common.enums.*;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.aiinterview.dto.*;
import com.hirehub.aiinterview.entity.AiInterview;
import com.hirehub.aiinterview.entity.AiInterviewQuestion;
import com.hirehub.aiinterview.repository.AiInterviewQuestionRepository;
import com.hirehub.aiinterview.repository.AiInterviewRepository;
import com.hirehub.job.entity.Job;
import com.hirehub.job.repository.JobRepository;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInterviewService {

    private final AiInterviewRepository interviewRepository;
    private final AiInterviewQuestionRepository questionRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AiResilienceService aiService;
    private final ObjectMapper objectMapper;

    // ── Start Interview ──

    @Transactional
    public AiInterviewResponse startInterview(Authentication auth, StartInterviewRequest request) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can start AI interviews");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", request.getJobId().toString()));

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Job is not active");
        }

        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", request.getResumeId().toString()));

        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only use your own resumes");
        }

        int totalQ = Integer.parseInt(request.getTotalQuestions());

        AiInterview interview = AiInterview.builder()
                .student(student)
                .job(job)
                .resume(resume)
                .interviewType(request.getInterviewType())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : AiInterviewDifficulty.MEDIUM)
                .totalQuestions(totalQ)
                .currentQuestionNumber(0)
                .status(AiInterviewStatus.IN_PROGRESS)
                .build();

        interview = interviewRepository.save(interview);

        // Generate first question
        AiInterviewQuestion firstQuestion = generateNextQuestion(interview, student, job, resume, null, null);
        interview.setCurrentQuestionNumber(1);
        interview.setStartedAt(java.time.OffsetDateTime.now());
        interview = interviewRepository.save(interview);

        return toResponse(interview, firstQuestion);
    }

    // ── Next Question ──

    @Transactional
    public AiInterviewQuestionResponse getNextQuestion(Authentication auth, java.util.UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        AiInterview interview = findAndAuthorizeInterview(user, interviewId);

        if (interview.getStatus() != AiInterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Interview is not in progress");
        }

        if (interview.getCurrentQuestionNumber() >= interview.getTotalQuestions()) {
            throw new IllegalStateException("All questions have been generated");
        }

        // Get previous context
        List<AiInterviewQuestion> existingQuestions = questionRepository
                .findByInterviewIdOrderByQuestionNumberAsc(interviewId);

        AiInterviewQuestion previousQuestion = existingQuestions.isEmpty() ? null :
                existingQuestions.getLast();
        String previousAnswer = previousQuestion != null ? previousQuestion.getStudentAnswer() : null;
        AiService.PreviousEvaluation previousEval = null;
        if (previousQuestion != null && previousQuestion.getScore() != null) {
            previousEval = new AiService.PreviousEvaluation(
                    previousQuestion.getScore(),
                    fromJson(previousQuestion.getStrengths()),
                    fromJson(previousQuestion.getWeaknesses()),
                    fromJson(previousQuestion.getMissingConcepts()));
        }

        Student student = interview.getStudent();
        AiInterviewQuestion question = generateNextQuestion(interview, student, interview.getJob(), interview.getResume(),
                previousQuestion != null ? previousQuestion : null, previousEval);

        interview.setCurrentQuestionNumber(interview.getCurrentQuestionNumber() + 1);
        interviewRepository.save(interview);

        return toQuestionResponse(question);
    }

    // ── Submit Answer ──

    @Transactional
    public AnswerEvaluationResponse submitAnswer(Authentication auth, java.util.UUID interviewId,
                                                  int questionNumber, SubmitAnswerRequest request) {
        User user = findUserByEmail(auth.getName());
        AiInterview interview = findAndAuthorizeInterview(user, interviewId);

        if (interview.getStatus() != AiInterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Interview is not in progress");
        }

        AiInterviewQuestion question = questionRepository
                .findByInterviewIdAndQuestionNumber(interviewId, questionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "number", String.valueOf(questionNumber)));

        if (question.getStudentAnswer() != null && !question.getStudentAnswer().isBlank()) {
            throw new IllegalStateException("This question already has an answer");
        }

        // Save answer
        question.setStudentAnswer(request.getStudentAnswer());
        questionRepository.save(question);

        // Evaluate with AI
        AnswerEvaluation evaluation;
        try {
            evaluation = aiService.evaluateMockInterviewAnswer(
                    question.getQuestion(),
                    question.getCategory().name(),
                    fromJson(question.getExpectedTopics()),
                    request.getStudentAnswer(),
                    interview.getJob().getTitle(),
                    fromJson(interview.getJob().getSkills()),
                    interview.getDifficulty().name());
        } catch (AiServiceException e) {
            throw new IllegalStateException("Answer evaluation failed: " + e.getMessage());
        }

        // Save evaluation
        question.setScore(clampScore(evaluation.getScore()));
        question.setStrengths(toJson(evaluation.getStrengths()));
        question.setWeaknesses(toJson(evaluation.getWeaknesses()));
        question.setFeedback(evaluation.getFeedback());
        question.setMissingConcepts(toJson(evaluation.getMissingConcepts()));
        question.setIdealAnswerPoints(toJson(evaluation.getIdealAnswerPoints()));
        questionRepository.save(question);

        return AnswerEvaluationResponse.builder()
                .score(question.getScore())
                .strengths(fromJson(question.getStrengths()))
                .weaknesses(fromJson(question.getWeaknesses()))
                .feedback(question.getFeedback())
                .missingConcepts(fromJson(question.getMissingConcepts()))
                .idealAnswerPoints(fromJson(question.getIdealAnswerPoints()))
                .build();
    }

    // ── Complete Interview ──

    @Transactional
    public AiInterviewResponse completeInterview(Authentication auth, java.util.UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        AiInterview interview = findAndAuthorizeInterview(user, interviewId);

        if (interview.getStatus() != AiInterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Interview is not in progress");
        }

        long answeredCount = questionRepository.countByInterviewIdAndStudentAnswerIsNotNull(interviewId);
        if (answeredCount < interview.getTotalQuestions()) {
            throw new IllegalStateException("All questions must be answered before completing");
        }

        // Build question results for report
        List<AiInterviewQuestion> questions = questionRepository
                .findByInterviewIdOrderByQuestionNumberAsc(interviewId);

        List<AiService.QuestionResult> results = questions.stream()
                .map(q -> new AiService.QuestionResult(
                        q.getQuestion(), q.getCategory().name(), q.getStudentAnswer(),
                        q.getScore() != null ? q.getScore() : 0,
                        fromJson(q.getStrengths()), fromJson(q.getWeaknesses()),
                        fromJson(q.getMissingConcepts())))
                .toList();

        // Generate report
        InterviewReport report;
        try {
            report = aiService.generateMockInterviewReport(
                    interview.getJob().getTitle(),
                    interview.getInterviewType().name(),
                    results);
        } catch (AiServiceException e) {
            throw new IllegalStateException("Report generation failed: " + e.getMessage());
        }

        // Deterministic category scores
        double techAvg = questions.stream()
                .filter(q -> q.getCategory() == QuestionCategory.TECHNICAL || q.getCategory() == QuestionCategory.PROJECT)
                .mapToInt(q -> q.getScore() != null ? q.getScore() : 0).average().orElse(report.getTechnicalScore());
        double commAvg = questions.stream()
                .filter(q -> q.getCategory() == QuestionCategory.BEHAVIORAL || q.getCategory() == QuestionCategory.HR)
                .mapToInt(q -> q.getScore() != null ? q.getScore() : 0).average().orElse(report.getCommunicationScore());
        double problemAvg = questions.stream()
                .mapToInt(q -> q.getScore() != null ? q.getScore() : 0).average().orElse(report.getProblemSolvingScore());

        interview.setOverallScore(clampScore(report.getOverallScore()));
        interview.setTechnicalScore(clampScore((int) Math.round(techAvg)));
        interview.setCommunicationScore(clampScore((int) Math.round(commAvg)));
        interview.setProblemSolvingScore(clampScore((int) Math.round(problemAvg)));
        interview.setStrengths(toJson(report.getStrongestAreas()));
        interview.setWeaknesses(toJson(report.getWeakestAreas()));
        interview.setMissingConcepts(toJson(report.getMissingConcepts()));
        interview.setRecommendedTopics(toJson(report.getRecommendedTopics()));
        interview.setOverallFeedback(report.getOverallFeedback());
        interview.setStatus(AiInterviewStatus.COMPLETED);
        interview.setCompletedAt(java.time.OffsetDateTime.now());

        interview = interviewRepository.save(interview);
        return toResponse(interview);
    }

    // ── Abandon Interview ──

    @Transactional
    public AiInterviewResponse abandonInterview(Authentication auth, java.util.UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        AiInterview interview = findAndAuthorizeInterview(user, interviewId);

        if (interview.getStatus() != AiInterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress interviews can be abandoned");
        }

        interview.setStatus(AiInterviewStatus.ABANDONED);
        interview = interviewRepository.save(interview);
        return toResponse(interview);
    }

    // ── Queries ──

    @Transactional(readOnly = true)
    public Page<AiInterviewResponse> getMyInterviews(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can view their interviews");
        }
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return interviewRepository.findByStudentIdOrderByCreatedAtDesc(student.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AiInterviewResponse getInterview(Authentication auth, java.util.UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        AiInterview interview = findAndAuthorizeInterview(user, interviewId);
        return toResponse(interview);
    }

    @Transactional(readOnly = true)
    public List<AiInterviewQuestionResponse> getInterviewQuestions(Authentication auth, java.util.UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        findAndAuthorizeInterview(user, interviewId); // ownership check

        return questionRepository.findByInterviewIdOrderByQuestionNumberAsc(interviewId)
                .stream().map(this::toQuestionResponse).toList();
    }

    @Transactional(readOnly = true)
    public AiInterviewResponse getInterviewReport(Authentication auth, java.util.UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        AiInterview interview = findAndAuthorizeInterview(user, interviewId);
        return toResponse(interview);
    }

    // ── Helpers ──

    private AiInterviewQuestion generateNextQuestion(AiInterview interview, Student student, Job job,
                                                      Resume resume, AiInterviewQuestion previousQ,
                                                      AiService.PreviousEvaluation previousEval) {
        int nextNumber = interview.getCurrentQuestionNumber() + 1;

        // Check idempotency
        var existing = questionRepository.findByInterviewIdAndQuestionNumber(interview.getId(), nextNumber);
        if (existing.isPresent()) return existing.get();

        // Build context
        List<AiService.QuestionContext> prevContext = new ArrayList<>();
        if (previousQ != null) {
            prevContext.add(new AiService.QuestionContext(previousQ.getQuestion(), previousQ.getCategory().name()));
        }

        GeneratedQuestion generated;
        try {
            generated = aiService.generateMockInterviewQuestion(
                    job.getTitle(),
                    job.getDescription(),
                    fromJson(job.getSkills()),
                    resume.getExtractedText() != null ? resume.getExtractedText() : "",
                    fromJson(student.getSkills()),
                    interview.getInterviewType().name(),
                    interview.getDifficulty().name(),
                    nextNumber,
                    interview.getTotalQuestions(),
                    prevContext,
                    previousQ != null ? previousQ.getStudentAnswer() : null,
                    previousEval);
        } catch (AiServiceException e) {
            throw new IllegalStateException("Question generation failed: " + e.getMessage());
        }

        QuestionCategory category;
        try {
            category = QuestionCategory.valueOf(generated.getCategory().toUpperCase());
        } catch (Exception e) {
            category = QuestionCategory.TECHNICAL;
        }

        AiInterviewQuestion question = AiInterviewQuestion.builder()
                .interview(interview)
                .questionNumber(nextNumber)
                .question(generated.getQuestion())
                .category(category)
                .expectedTopics(toJson(generated.getExpectedTopics()))
                .build();

        return questionRepository.save(question);
    }

    private AiInterview findAndAuthorizeInterview(User user, java.util.UUID interviewId) {
        AiInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Interview", "id", interviewId.toString()));

        if (user.getRole() == Role.ADMIN) return interview;

        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Access denied");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        if (!interview.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only access your own interviews");
        }

        return interview;
    }

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

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private AiInterviewResponse toResponse(AiInterview interview) {
        return toResponse(interview, null);
    }

    private AiInterviewResponse toResponse(AiInterview interview, AiInterviewQuestion firstQuestion) {
        return AiInterviewResponse.builder()
                .id(interview.getId())
                .jobId(interview.getJob() != null ? interview.getJob().getId() : null)
                .jobTitle(interview.getJob() != null ? interview.getJob().getTitle() : null)
                .companyName(interview.getJob() != null && interview.getJob().getCompany() != null ?
                        interview.getJob().getCompany().getName() : null)
                .resumeId(interview.getResume() != null ? interview.getResume().getId() : null)
                .interviewType(interview.getInterviewType())
                .difficulty(interview.getDifficulty())
                .totalQuestions(interview.getTotalQuestions())
                .currentQuestionNumber(interview.getCurrentQuestionNumber())
                .status(interview.getStatus())
                .overallScore(interview.getOverallScore())
                .technicalScore(interview.getTechnicalScore())
                .communicationScore(interview.getCommunicationScore())
                .problemSolvingScore(interview.getProblemSolvingScore())
                .strengths(fromJson(interview.getStrengths()))
                .weaknesses(fromJson(interview.getWeaknesses()))
                .missingConcepts(fromJson(interview.getMissingConcepts()))
                .recommendedTopics(fromJson(interview.getRecommendedTopics()))
                .overallFeedback(interview.getOverallFeedback())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }

    private AiInterviewQuestionResponse toQuestionResponse(AiInterviewQuestion q) {
        return AiInterviewQuestionResponse.builder()
                .id(q.getId())
                .questionNumber(q.getQuestionNumber())
                .question(q.getQuestion())
                .category(q.getCategory())
                .expectedTopics(fromJson(q.getExpectedTopics()))
                .studentAnswer(q.getStudentAnswer())
                .score(q.getScore())
                .strengths(fromJson(q.getStrengths()))
                .weaknesses(fromJson(q.getWeaknesses()))
                .feedback(q.getFeedback())
                .missingConcepts(fromJson(q.getMissingConcepts()))
                .idealAnswerPoints(fromJson(q.getIdealAnswerPoints()))
                .build();
    }
}
