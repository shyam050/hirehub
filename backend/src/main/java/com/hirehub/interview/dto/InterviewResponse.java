package com.hirehub.interview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hirehub.common.enums.InterviewStatus;
import com.hirehub.common.enums.InterviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewResponse {
    private UUID id;
    private UUID applicationId;
    private UUID jobId;
    private String jobTitle;
    private UUID companyId;
    private String companyName;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private InterviewType interviewType;
    private OffsetDateTime scheduledAt;
    private Integer duration;
    private String meetingLink;
    private String interviewerName;
    private InterviewStatus status;
    private String notes;
    private String feedback;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
