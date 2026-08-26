package com.hirehub.aiinterview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hirehub.common.enums.AiInterviewDifficulty;
import com.hirehub.common.enums.AiInterviewStatus;
import com.hirehub.common.enums.AiInterviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInterviewResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private UUID resumeId;
    private AiInterviewType interviewType;
    private AiInterviewDifficulty difficulty;
    private int totalQuestions;
    private int currentQuestionNumber;
    private AiInterviewStatus status;
    private Integer overallScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingConcepts;
    private List<String> recommendedTopics;
    private String overallFeedback;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
