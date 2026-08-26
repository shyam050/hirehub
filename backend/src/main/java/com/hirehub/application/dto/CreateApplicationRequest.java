package com.hirehub.application.dto;

import com.hirehub.common.enums.ApplicationStage;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateApplicationRequest {

    private UUID jobId;

    @Size(max = 5000, message = "Cover letter must not exceed 5000 characters")
    private String coverLetter;
}
