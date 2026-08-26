package com.hirehub.resumeanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ResumeAnalysisResponse {
    private UUID id;
    private UUID resumeId;
    private String resumeFileName;
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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
