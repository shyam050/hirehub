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
public class ResumeAnalysisResult {
    private int overallScore;
    private List<String> extractedSkills;
    private List<String> extractedEducation;
    private List<String> extractedProjects;
    private List<String> extractedExperience;
    private List<String> extractedCertifications;
    private List<String> extractedAchievements;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingSkills;
    private List<String> recommendations;
}
