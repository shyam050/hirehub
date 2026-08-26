package com.hirehub.job.dto;

import com.hirehub.common.enums.JobStatus;
import com.hirehub.common.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class JobResponse {
    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID postedBy;
    private String title;
    private String description;
    private String location;
    private JobType jobType;
    private Boolean remote;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private BigDecimal experienceMin;
    private BigDecimal experienceMax;
    private String educationRequired;
    private String requirements;
    private String skills;
    private OffsetDateTime applicationDeadline;
    private JobStatus status;
    private Integer applicationCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
