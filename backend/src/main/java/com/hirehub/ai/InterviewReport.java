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
public class InterviewReport {
    private int overallScore;
    private int technicalScore;
    private int communicationScore;
    private int problemSolvingScore;
    private List<String> strongestAreas;
    private List<String> weakestAreas;
    private List<String> missingConcepts;
    private List<String> recommendedTopics;
    private String overallFeedback;
}
