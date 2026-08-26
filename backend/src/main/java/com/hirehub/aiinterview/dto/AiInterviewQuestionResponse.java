package com.hirehub.aiinterview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hirehub.common.enums.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInterviewQuestionResponse {
    private UUID id;
    private int questionNumber;
    private String question;
    private QuestionCategory category;
    private List<String> expectedTopics;
    private String studentAnswer;
    private Integer score;
    private List<String> strengths;
    private List<String> weaknesses;
    private String feedback;
    private List<String> missingConcepts;
    private List<String> idealAnswerPoints;
}
