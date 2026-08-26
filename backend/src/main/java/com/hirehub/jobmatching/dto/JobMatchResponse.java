package com.hirehub.jobmatching.dto;

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
public class JobMatchResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private UUID resumeId;
    private String resumeFileName;
    private int matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> recommendations;
    private String explanation;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
