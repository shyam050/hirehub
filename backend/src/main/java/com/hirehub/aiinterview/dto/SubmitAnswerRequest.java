package com.hirehub.aiinterview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotBlank(message = "Answer is required")
    @Size(min = 1, max = 5000, message = "Answer must be between 1 and 5000 characters")
    private String studentAnswer;
}
