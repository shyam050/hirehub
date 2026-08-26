package com.hirehub.aiinterview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerEvaluationResponse {
    private int score;
    private List<String> strengths;
    private List<String> weaknesses;
    private String feedback;
    private List<String> missingConcepts;
    private List<String> idealAnswerPoints;
}
