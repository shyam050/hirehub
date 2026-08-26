package com.hirehub.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitFeedbackRequest {

    @NotBlank(message = "Feedback is required")
    @Size(max = 5000, message = "Feedback cannot exceed 5000 characters")
    private String feedback;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}
