package com.hirehub.aiinterview.dto;

import com.hirehub.common.enums.AiInterviewDifficulty;
import com.hirehub.common.enums.AiInterviewType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartInterviewRequest {

    @NotNull(message = "Job ID is required")
    private UUID jobId;

    @NotNull(message = "Resume ID is required")
    private UUID resumeId;

    @NotNull(message = "Interview type is required")
    private AiInterviewType interviewType;

    private AiInterviewDifficulty difficulty = AiInterviewDifficulty.MEDIUM;

    @NotNull(message = "Total questions is required")
    @Pattern(regexp = "5|10", message = "Total questions must be 5 or 10")
    private String totalQuestions = "5";
}
