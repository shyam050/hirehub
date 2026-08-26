package com.hirehub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthCallbackRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;

    private String redirectUri;
}
