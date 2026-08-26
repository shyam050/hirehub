package com.hirehub.application.dto;

import com.hirehub.common.enums.ApplicationStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private UUID companyId;
    private String companyName;
    private ApplicationStage status;
    private String coverLetter;
    private String timeline;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
