package com.hirehub.interview.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleInterviewRequest {

    @NotNull(message = "New scheduled date is required")
    @Future(message = "Interview must be scheduled for the future")
    private OffsetDateTime scheduledAt;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 8 hours")
    private Integer duration;

    @Size(max = 1000, message = "Meeting link cannot exceed 1000 characters")
    private String meetingLink;

    @Size(max = 255, message = "Interviewer name cannot exceed 255 characters")
    private String interviewerName;
}
