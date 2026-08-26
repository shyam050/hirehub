package com.hirehub.job.dto;

import com.hirehub.common.enums.JobStatus;
import com.hirehub.common.enums.JobType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CreateJobRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 10000, message = "Description must not exceed 10000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    private Boolean remote = false;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private BigDecimal experienceMin;
    private BigDecimal experienceMax;

    @Size(max = 255, message = "Education requirement must not exceed 255 characters")
    private String educationRequired;

    @NotNull(message = "Application deadline is required")
    @Future(message = "Application deadline must be in the future")
    private OffsetDateTime applicationDeadline;

    private String requirements;
    private String skills;
}
