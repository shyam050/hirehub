package com.hirehub.interview.dto;

import com.hirehub.common.enums.InterviewType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleInterviewRequest {

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    @NotNull(message = "Interview type is required")
    private InterviewType interviewType;

    @NotNull(message = "Scheduled date is required")
    @Future(message = "Interview must be scheduled for the future")
    private OffsetDateTime scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 8 hours")
    private Integer duration;

    @Size(max = 1000, message = "Meeting link cannot exceed 1000 characters")
    private String meetingLink;

    @Size(max = 255, message = "Interviewer name cannot exceed 255 characters")
    private String interviewerName;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}
