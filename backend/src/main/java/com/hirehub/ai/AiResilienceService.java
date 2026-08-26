package com.hirehub.ai;

import com.hirehub.config.MetricsService;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Wraps AiService calls with Resilience4j circuit breaker + retry.
 * Delegates to the real OpenAiService and records metrics.
 */
@Slf4j
@Service
public class AiResilienceService {

    private final AiService aiService;
    private final MetricsService metrics;

    public AiResilienceService(AiService aiService, MetricsService metrics) {
        this.aiService = aiService;
        this.metrics = metrics;
    }

    /**
     * Resilient wrapper for resume analysis.
     * Circuit breaker: opens after configured failure rate.
     * Retry: retries transient failures with exponential backoff.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "aiService", fallbackMethod = "resumeAnalysisFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "aiService")
    public ResumeAnalysisResult analyzeResume(String resumeText) throws AiServiceException {
        Timer.Sample sample = metrics.startAiTimer();
        try {
            ResumeAnalysisResult result = aiService.analyzeResume(resumeText);
            metrics.recordResumeAnalysis();
            return result;
        } catch (AiServiceException e) {
            metrics.recordAiFailure();
            throw e;
        } finally {
            metrics.recordAiTimer(sample);
        }
    }

    /**
     * Resilient wrapper for job matching.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "aiService", fallbackMethod = "jobMatchFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "aiService")
    public JobMatchAnalysisResult analyzeJobMatch(String studentContext, String jobContext) throws AiServiceException {
        Timer.Sample sample = metrics.startAiTimer();
        try {
            JobMatchAnalysisResult result = aiService.analyzeJobMatch(studentContext, jobContext);
            metrics.recordJobMatching();
            return result;
        } catch (AiServiceException e) {
            metrics.recordAiFailure();
            throw e;
        } finally {
            metrics.recordAiTimer(sample);
        }
    }

    /**
     * Resilient wrapper for mock interview question generation.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "aiService", fallbackMethod = "questionFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "aiService")
    public GeneratedQuestion generateMockInterviewQuestion(
            String jobTitle, String jobDescription, java.util.List<String> requiredSkills,
            String resumeExcerpt, java.util.List<String> studentSkills, String interviewType,
            String difficulty, int questionNumber, int totalQuestions,
            java.util.List<AiService.QuestionContext> previousQuestions, String previousAnswer,
            AiService.PreviousEvaluation previousEvaluation) throws AiServiceException {
        Timer.Sample sample = metrics.startAiTimer();
        try {
            GeneratedQuestion q = aiService.generateMockInterviewQuestion(
                    jobTitle, jobDescription, requiredSkills, resumeExcerpt, studentSkills,
                    interviewType, difficulty, questionNumber, totalQuestions,
                    previousQuestions, previousAnswer, previousEvaluation);
            metrics.recordMockInterview();
            return q;
        } catch (AiServiceException e) {
            metrics.recordAiFailure();
            throw e;
        } finally {
            metrics.recordAiTimer(sample);
        }
    }

    /**
     * Resilient wrapper for answer evaluation.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "aiService", fallbackMethod = "evaluationFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "aiService")
    public AnswerEvaluation evaluateMockInterviewAnswer(
            String question, String category, java.util.List<String> expectedTopics,
            String studentAnswer, String jobTitle, java.util.List<String> requiredSkills,
            String difficulty) throws AiServiceException {
        Timer.Sample sample = metrics.startAiTimer();
        try {
            AnswerEvaluation eval = aiService.evaluateMockInterviewAnswer(
                    question, category, expectedTopics, studentAnswer, jobTitle,
                    requiredSkills, difficulty);
            metrics.recordMockInterview();
            return eval;
        } catch (AiServiceException e) {
            metrics.recordAiFailure();
            throw e;
        } finally {
            metrics.recordAiTimer(sample);
        }
    }

    /**
     * Resilient wrapper for interview report generation.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "aiService", fallbackMethod = "reportFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = "aiService")
    public InterviewReport generateMockInterviewReport(
            String jobTitle, String interviewType,
            java.util.List<AiService.QuestionResult> questionResults) throws AiServiceException {
        Timer.Sample sample = metrics.startAiTimer();
        try {
            InterviewReport report = aiService.generateMockInterviewReport(
                    jobTitle, interviewType, questionResults);
            metrics.recordMockInterview();
            return report;
        } catch (AiServiceException e) {
            metrics.recordAiFailure();
            throw e;
        } finally {
            metrics.recordAiTimer(sample);
        }
    }

    // ── Fallback methods ──

    private ResumeAnalysisResult resumeAnalysisFallback(String resumeText, Throwable t) {
        log.error("Circuit breaker fallback for resume analysis: {}", t.getMessage());
        throw new AiServiceException("AI service is temporarily unavailable. Please try again later.", t);
    }

    private JobMatchAnalysisResult jobMatchFallback(String studentContext, String jobContext, Throwable t) {
        log.error("Circuit breaker fallback for job matching: {}", t.getMessage());
        throw new AiServiceException("AI service is temporarily unavailable. Please try again later.", t);
    }

    private GeneratedQuestion questionFallback(String jobTitle, String jobDescription,
            java.util.List<String> requiredSkills, String resumeExcerpt,
            java.util.List<String> studentSkills, String interviewType,
            String difficulty, int questionNumber, int totalQuestions,
            java.util.List<AiService.QuestionContext> previousQuestions,
            String previousAnswer, AiService.PreviousEvaluation previousEvaluation, Throwable t) {
        log.error("Circuit breaker fallback for question generation: {}", t.getMessage());
        throw new AiServiceException("AI service is temporarily unavailable. Please try again later.", t);
    }

    private AnswerEvaluation evaluationFallback(String question, String category,
            java.util.List<String> expectedTopics, String studentAnswer,
            String jobTitle, java.util.List<String> requiredSkills,
            String difficulty, Throwable t) {
        log.error("Circuit breaker fallback for answer evaluation: {}", t.getMessage());
        throw new AiServiceException("AI service is temporarily unavailable. Please try again later.", t);
    }

    private InterviewReport reportFallback(String jobTitle, String interviewType,
            java.util.List<AiService.QuestionResult> questionResults, Throwable t) {
        log.error("Circuit breaker fallback for interview report: {}", t.getMessage());
        throw new AiServiceException("AI service is temporarily unavailable. Please try again later.", t);
    }
}
