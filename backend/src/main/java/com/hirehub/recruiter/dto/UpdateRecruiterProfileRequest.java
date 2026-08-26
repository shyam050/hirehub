package com.hirehub.recruiter.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRecruiterProfileRequest {

    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;
}
