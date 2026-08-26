package com.hirehub.application.dto;

import com.hirehub.common.enums.ApplicationStage;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class UpdateApplicationStatusRequest {

    @NotNull(message = "Status is required")
    private ApplicationStage status;

    private String note;
}
