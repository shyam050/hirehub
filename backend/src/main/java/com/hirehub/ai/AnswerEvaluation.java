package com.hirehub.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerEvaluation {
    private int score;
    private List<String> strengths;
    private List<String> weaknesses;
    private String feedback;
    private List<String> missingConcepts;
    private List<String> idealAnswerPoints;
}
