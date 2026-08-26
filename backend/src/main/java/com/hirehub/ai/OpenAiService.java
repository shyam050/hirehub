package com.hirehub.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI implementation of AiService.
 * Uses structured JSON output and validates all responses.
 */
@Slf4j
@Service
public class OpenAiService implements AiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    private static final int MAX_TEXT_LENGTH = 8000;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final String ANALYSIS_PROMPT = """
        You are an expert resume analyzer for a campus recruitment platform.

        Analyze the following resume text and return a JSON object with EXACTLY this structure:

        {
          "overallScore": <number 0-100>,
          "extractedSkills": [<list of technical and soft skills found>],
          "extractedEducation": [<education entries found>],
          "extractedProjects": [<project descriptions found>],
          "extractedExperience": [<work experience entries found>],
          "extractedCertifications": [<certifications found>],
          "extractedAchievements": [<achievements and awards found>],
          "strengths": [<3-5 specific strengths based on this resume>],
          "weaknesses": [<3-5 specific weaknesses based on this resume>],
          "missingSkills": [<potentially relevant skills not present in the resume>],
          "recommendations": [<3-5 actionable, specific recommendations for improvement>]
        }

        SCORING CRITERIA:
        - Skills breadth and relevance (0-20)
        - Education quality and completeness (0-15)
        - Projects depth and impact (0-20)
        - Work experience relevance (0-15)
        - Certifications and achievements (0-10)
        - Resume clarity and structure (0-10)
        - Completeness of information (0-10)

        RULES:
        - Return ONLY valid JSON, no markdown, no code fences, no explanation before or after.
        - Do NOT make up information that isn't in the resume.
        - Do NOT claim a skill is missing if it is clearly present.
        - Recommendations must be specific and actionable, not generic.
        - All arrays should have meaningful content (minimum 3 items where applicable).
        - The overallScore must be an integer between 0 and 100.
        - Do NOT include skills, education, or projects that aren't actually in the resume text.
        """;

    private static final String MATCH_PROMPT = """
        You are an expert job-matching engine for a campus recruitment platform.

        Given a student profile and a job posting, evaluate how well the student matches the role.

        Return a JSON object with EXACTLY this structure:
        {
          "aiScore": <number 0-100>,
          "matchedSkills": [<skills the student has that match the job requirements>],
          "missingSkills": [<required/preferred skills the student is missing>],
          "strengths": [<3-5 specific strengths the student brings to this role>],
          "recommendations": [<2-3 actionable suggestions to improve the match>],
          "explanation": <2-3 sentence explanation of the match quality>
        }

        RULES:
        - Return ONLY valid JSON, no markdown, no code fences.
        - Do NOT invent skills the student doesn't have.
        - matchedSkills must only include skills explicitly listed in the student profile or resume.
        - missingSkills must only include skills explicitly required or preferred in the job posting.
        - Recommendations must be specific and actionable, not generic.
        - The aiScore must be an integer between 0 and 100.
        - The explanation must reference specific aspects of both the student and the job.
        - Use ONLY the supplied information. Do not fabricate qualifications.
        """;

    public OpenAiService(
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-4o}") String model,
            @Value("${ai.openai.base-url:https://api.openai.com}") String baseUrl) {
        this.model = model;
        this.objectMapper = new ObjectMapper();

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ResumeAnalysisResult analyzeResume(String resumeText) throws AiServiceException {
        if (resumeText == null || resumeText.isBlank()) {
            throw new AiServiceException("Resume text is empty. Cannot analyze an empty document.");
        }

        String truncatedText = resumeText.length() > MAX_TEXT_LENGTH
                ? resumeText.substring(0, MAX_TEXT_LENGTH)
                : resumeText;

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.3,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", ANALYSIS_PROMPT),
                            Map.of("role", "user", "content", "Analyze this resume:\n\n" + truncatedText)
                    )
            );

            String responseJson = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            return parseResponse(responseJson);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new AiServiceException("AI service rate limit exceeded. Please try again later.");
            }
            log.error("OpenAI API error: status={}", e.getStatusCode().value());
            throw new AiServiceException("AI service request failed. Please try again later.");
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI service error", e);
            throw new AiServiceException("AI analysis failed. Please try again later.", e);
        }
    }

    @Override
    public JobMatchAnalysisResult analyzeJobMatch(String studentContext, String jobContext) throws AiServiceException {
        if (studentContext == null || studentContext.isBlank()) {
            throw new AiServiceException("Student context is empty.");
        }
        if (jobContext == null || jobContext.isBlank()) {
            throw new AiServiceException("Job context is empty.");
        }

        String truncatedStudent = studentContext.length() > 4000
                ? studentContext.substring(0, 4000) : studentContext;
        String truncatedJob = jobContext.length() > 3000
                ? jobContext.substring(0, 3000) : jobContext;

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", MATCH_PROMPT),
                            Map.of("role", "user", "content",
                                    "STUDENT PROFILE:\n" + truncatedStudent +
                                    "\n\nJOB POSTING:\n" + truncatedJob)
                    )
            );

            String responseJson = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            return parseMatchResponse(responseJson);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new AiServiceException("AI service rate limit exceeded. Please try again later.");
            }
            log.error("OpenAI API error: status={}", e.getStatusCode().value());
            throw new AiServiceException("AI service request failed. Please try again later.");
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI service error during job matching", e);
            throw new AiServiceException("AI job matching failed. Please try again later.", e);
        }
    }

    private ResumeAnalysisResult parseResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            if (content == null || content.isBlank()) {
                throw new AiServiceException("AI provider returned an empty response.");
            }

            JsonNode parsed = objectMapper.readTree(content);

            return ResumeAnalysisResult.builder()
                    .overallScore(clampScore(parsed.path("overallScore").asInt(0)))
                    .extractedSkills(toStringList(parsed.path("extractedSkills")))
                    .extractedEducation(toStringList(parsed.path("extractedEducation")))
                    .extractedProjects(toStringList(parsed.path("extractedProjects")))
                    .extractedExperience(toStringList(parsed.path("extractedExperience")))
                    .extractedCertifications(toStringList(parsed.path("extractedCertifications")))
                    .extractedAchievements(toStringList(parsed.path("extractedAchievements")))
                    .strengths(toStringList(parsed.path("strengths")))
                    .weaknesses(toStringList(parsed.path("weaknesses")))
                    .missingSkills(toStringList(parsed.path("missingSkills")))
                    .recommendations(toStringList(parsed.path("recommendations")))
                    .build();
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new AiServiceException("AI provider returned an invalid response.");
        }
    }

    private JobMatchAnalysisResult parseMatchResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            if (content == null || content.isBlank()) {
                throw new AiServiceException("AI provider returned an empty response.");
            }

            JsonNode parsed = objectMapper.readTree(content);

            return JobMatchAnalysisResult.builder()
                    .aiScore(clampScore(parsed.path("aiScore").asInt(0)))
                    .matchedSkills(toStringList(parsed.path("matchedSkills")))
                    .missingSkills(toStringList(parsed.path("missingSkills")))
                    .strengths(toStringList(parsed.path("strengths")))
                    .recommendations(toStringList(parsed.path("recommendations")))
                    .explanation(parsed.path("explanation").isTextual()
                            ? parsed.path("explanation").asText() : "No explanation available.")
                    .build();
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse job match AI response", e);
            throw new AiServiceException("AI provider returned an invalid response.");
        }
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String value = item.asText().trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }
    private static final String INTERVIEW_SYSTEM_PROMPT = """
        You are an expert technical interviewer for a campus recruitment platform.
        You conduct structured mock interviews for students preparing for job interviews.
        
        You must:
        - Ask relevant, role-specific questions
        - Evaluate answers objectively and fairly
        - Provide constructive feedback
        - Adapt difficulty based on student performance
        - Return ONLY valid JSON with no markdown or explanation outside the JSON
        """;

    private String getDifficultyLabel(String difficulty) {
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> "Basic introductory level";
            case "HARD" -> "Advanced expert level with deep technical depth";
            default -> "Intermediate level";
        };
    }

    private String getInterviewTypeContext(String type) {
        return switch (type.toUpperCase()) {
            case "TECHNICAL" -> "Focus on technical skills, coding concepts, system design, and problem-solving abilities.";
            case "HR" -> "Focus on soft skills, career goals, cultural fit, strengths/weaknesses, and professional behavior.";
            case "BEHAVIORAL" -> "Focus on past experiences, teamwork, leadership, conflict resolution, and how the candidate handles challenges (STAR method).";
            case "MIXED" -> "Balance technical questions with behavioral/HR questions. Mix both types throughout the interview.";
            default -> "Ask a balanced mix of technical and behavioral questions.";
        };
    }

    @Override
    public GeneratedQuestion generateMockInterviewQuestion(
            String jobTitle, String jobDescription, List<String> requiredSkills,
            String resumeExcerpt, List<String> studentSkills, String interviewType,
            String difficulty, int questionNumber, int totalQuestions,
            List<QuestionContext> previousQuestions, String previousAnswer,
            PreviousEvaluation previousEvaluation) throws AiServiceException {

        StringBuilder context = new StringBuilder();
        context.append("INTERVIEW CONTEXT:\n");
        context.append("Job: ").append(jobTitle).append("\n");
        context.append("Job Description: ").append(truncate(jobDescription, 2000)).append("\n");
        context.append("Required Skills: ").append(String.join(", ", requiredSkills)).append("\n");
        context.append("Student Skills: ").append(String.join(", ", studentSkills)).append("\n");
        context.append("Resume Excerpt: ").append(truncate(resumeExcerpt, 3000)).append("\n\n");
        context.append("INTERVIEW PARAMETERS:\n");
        context.append("Type: ").append(interviewType.toUpperCase()).append("\n");
        context.append("Difficulty: ").append(getDifficultyLabel(difficulty)).append("\n");
        context.append("Question: ").append(questionNumber).append(" of ").append(totalQuestions).append("\n\n");
        context.append(getInterviewTypeContext(interviewType)).append("\n\n");

        if (!previousQuestions.isEmpty()) {
            context.append("PREVIOUS QUESTIONS:\n");
            for (int i = 0; i < previousQuestions.size(); i++) {
                var q = previousQuestions.get(i);
                context.append(i + 1).append(". [").append(q.category()).append("] ").append(q.question()).append("\n");
            }
            context.append("\n");
        }
        if (previousAnswer != null) {
            context.append("PREVIOUS ANSWER: ").append(previousAnswer).append("\n\n");
        }
        if (previousEvaluation != null) {
            context.append("PREVIOUS EVALUATION: Score ").append(previousEvaluation.score()).append("/100. ");
            context.append("Strengths: ").append(String.join(", ", previousEvaluation.strengths())).append(". ");
            context.append("Weaknesses: ").append(String.join(", ", previousEvaluation.weaknesses())).append(". ");
            context.append("Missing: ").append(String.join(", ", previousEvaluation.missingConcepts())).append(".\n\n");
        }

        if (previousEvaluation != null && previousEvaluation.score() >= 75) {
            context.append("ADAPTIVE: Student answered well. Increase difficulty.\n");
        } else if (previousEvaluation != null && previousEvaluation.score() < 50) {
            context.append("ADAPTIVE: Student struggled. Ask a simpler question.\n");
        }

        String userPrompt = context + "\nGenerate exactly one interview question. Return JSON:\n" +
                "{\"question\": \"...\", \"category\": \"technical|behavioral|hr|project|resume\", " +
                "\"expectedTopics\": [...]}";

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.4,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", INTERVIEW_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String responseJson = callOpenAi(requestBody);
            return parseQuestionResponse(responseJson);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate interview question", e);
            throw new AiServiceException("Question generation failed.", e);
        }
    }

    @Override
    public AnswerEvaluation evaluateMockInterviewAnswer(
            String question, String category, List<String> expectedTopics,
            String studentAnswer, String jobTitle, List<String> requiredSkills,
            String difficulty) throws AiServiceException {

        String userPrompt = "Evaluate this interview answer.\n\n" +
                "QUESTION: " + question + "\n" +
                "CATEGORY: " + category + "\n" +
                "EXPECTED TOPICS: " + String.join(", ", expectedTopics) + "\n" +
                "DIFFICULTY: " + difficulty + "\n" +
                "JOB: " + jobTitle + " (requires: " + String.join(", ", requiredSkills) + ")\n\n" +
                "STUDENT ANSWER:\n" + studentAnswer + "\n\n" +
                "Return JSON:\n" +
                "{\"score\": <0-100>, \"strengths\": [...], \"weaknesses\": [...], " +
                "\"feedback\": \"...\", \"missingConcepts\": [...], \"idealAnswerPoints\": [...]}";

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", INTERVIEW_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String responseJson = callOpenAi(requestBody);
            return parseEvaluationResponse(responseJson);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to evaluate interview answer", e);
            throw new AiServiceException("Answer evaluation failed.", e);
        }
    }

    @Override
    public InterviewReport generateMockInterviewReport(
            String jobTitle, String interviewType,
            List<QuestionResult> questionResults) throws AiServiceException {

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < questionResults.size(); i++) {
            var q = questionResults.get(i);
            summary.append("\nQ").append(i + 1).append(" [").append(q.category()).append("] (Score: ").append(q.score()).append("):\n");
            summary.append("Question: ").append(q.question()).append("\n");
            summary.append("Answer: ").append(q.studentAnswer()).append("\n");
            summary.append("Strengths: ").append(String.join(", ", q.strengths())).append("\n");
            summary.append("Weaknesses: ").append(String.join(", ", q.weaknesses())).append("\n");
        }

        double avgScore = questionResults.stream().mapToInt(QuestionResult::score).average().orElse(0);

        String userPrompt = "Generate a final interview report.\n\n" +
                "JOB: " + jobTitle + "\nTYPE: " + interviewType.toUpperCase() + "\n\n" +
                "QUESTION RESULTS:" + summary + "\n\n" +
                "AVERAGE SCORE: " + Math.round(avgScore) + "/100\n\n" +
                "Return JSON:\n" +
                "{\"overallScore\": <0-100>, \"technicalScore\": <0-100>, " +
                "\"communicationScore\": <0-100>, \"problemSolvingScore\": <0-100>, " +
                "\"strongestAreas\": [...], \"weakestAreas\": [...], " +
                "\"missingConcepts\": [...], \"recommendedTopics\": [...], " +
                "\"overallFeedback\": \"...\"}";

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", INTERVIEW_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String responseJson = callOpenAi(requestBody);
            return parseReportResponse(responseJson);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate interview report", e);
            throw new AiServiceException("Report generation failed.", e);
        }
    }

    // ── Shared helpers ──

    private String callOpenAi(Map<String, Object> requestBody) {
        try {
            return webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new AiServiceException("AI service rate limit exceeded.");
            }
            throw new AiServiceException("AI service request failed.");
        } catch (Exception e) {
            throw new AiServiceException("AI service unavailable.", e);
        }
    }

    private GeneratedQuestion parseQuestionResponse(String responseJson) {
        try {
            String content = extractContent(responseJson);
            JsonNode parsed = objectMapper.readTree(content);

            String category = parsed.path("category").asText("technical");
            List<String> validCategories = List.of("technical", "behavioral", "hr", "project", "resume");
            if (!validCategories.contains(category.toLowerCase())) category = "technical";

            return GeneratedQuestion.builder()
                    .question(parsed.path("question").isTextual() ? parsed.path("question").asText() : "Tell me about yourself.")
                    .category(category)
                    .expectedTopics(toStringList(parsed.path("expectedTopics")))
                    .build();
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Invalid question response from AI.");
        }
    }

    private AnswerEvaluation parseEvaluationResponse(String responseJson) {
        try {
            String content = extractContent(responseJson);
            JsonNode parsed = objectMapper.readTree(content);

            return AnswerEvaluation.builder()
                    .score(clampScore(parsed.path("score").asInt(0)))
                    .strengths(toStringList(parsed.path("strengths")))
                    .weaknesses(toStringList(parsed.path("weaknesses")))
                    .feedback(parsed.path("feedback").isTextual() ? parsed.path("feedback").asText() : "No feedback.")
                    .missingConcepts(toStringList(parsed.path("missingConcepts")))
                    .idealAnswerPoints(toStringList(parsed.path("idealAnswerPoints")))
                    .build();
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Invalid evaluation response from AI.");
        }
    }

    private InterviewReport parseReportResponse(String responseJson) {
        try {
            String content = extractContent(responseJson);
            JsonNode parsed = objectMapper.readTree(content);

            return InterviewReport.builder()
                    .overallScore(clampScore(parsed.path("overallScore").asInt(0)))
                    .technicalScore(clampScore(parsed.path("technicalScore").asInt(0)))
                    .communicationScore(clampScore(parsed.path("communicationScore").asInt(0)))
                    .problemSolvingScore(clampScore(parsed.path("problemSolvingScore").asInt(0)))
                    .strongestAreas(toStringList(parsed.path("strongestAreas")))
                    .weakestAreas(toStringList(parsed.path("weakestAreas")))
                    .missingConcepts(toStringList(parsed.path("missingConcepts")))
                    .recommendedTopics(toStringList(parsed.path("recommendedTopics")))
                    .overallFeedback(parsed.path("overallFeedback").isTextual() ? parsed.path("overallFeedback").asText() : "No feedback.")
                    .build();
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Invalid report response from AI.");
        }
    }

    private String extractContent(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new AiServiceException("AI provider returned an empty response.");
            }
            return content;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to parse AI response.");
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }
}
