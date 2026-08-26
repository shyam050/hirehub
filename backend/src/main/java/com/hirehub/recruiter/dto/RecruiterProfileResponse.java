package com.hirehub.recruiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class RecruiterProfileResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String jobTitle;
    private String phone;
    private String bio;
    private UUID companyId;
    private String companyName;
    private OffsetDateTime createdAt;
}
