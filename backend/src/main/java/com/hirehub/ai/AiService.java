package com.hirehub.ai;

import java.util.List;

/**
 * AI service abstraction for resume analysis, job matching, and mock interviews.
 * Implementations can use OpenAI, Azure OpenAI, or any other provider.
 */
public interface AiService {

    /**
     * Analyze resume text and return structured analysis.
     */
    ResumeAnalysisResult analyzeResume(String resumeText) throws AiServiceException;

    /**
     * Calculate job match between student profile and job posting.
     */
    JobMatchAnalysisResult analyzeJobMatch(String studentContext, String jobContext) throws AiServiceException;

    /**
     * Generate a mock interview question based on context.
     */
    GeneratedQuestion generateMockInterviewQuestion(
            String jobTitle,
            String jobDescription,
            List<String> requiredSkills,
            String resumeExcerpt,
            List<String> studentSkills,
            String interviewType,
            String difficulty,
            int questionNumber,
            int totalQuestions,
            List<QuestionContext> previousQuestions,
            String previousAnswer,
            PreviousEvaluation previousEvaluation
    ) throws AiServiceException;

    /**
     * Evaluate a mock interview answer.
     */
    AnswerEvaluation evaluateMockInterviewAnswer(
            String question,
            String category,
            List<String> expectedTopics,
            String studentAnswer,
            String jobTitle,
            List<String> requiredSkills,
            String difficulty
    ) throws AiServiceException;

    /**
     * Generate final interview report.
     */
    InterviewReport generateMockInterviewReport(
            String jobTitle,
            String interviewType,
            List<QuestionResult> questionResults
    ) throws AiServiceException;

    /**
     * Context for a previously asked question.
     */
    record QuestionContext(String question, String category) {}

    /**
     * Previous evaluation context for adaptive questioning.
     */
    record PreviousEvaluation(int score, List<String> strengths, List<String> weaknesses, List<String> missingConcepts) {}

    /**
     * Result of a completed question for the final report.
     */
    record QuestionResult(
            String question,
            String category,
            String studentAnswer,
            int score,
            List<String> strengths,
            List<String> weaknesses,
            List<String> missingConcepts
    ) {}
}
