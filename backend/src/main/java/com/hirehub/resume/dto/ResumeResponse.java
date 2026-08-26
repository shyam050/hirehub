package com.hirehub.resume.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ResumeResponse {

    private UUID id;
    private UUID studentId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Boolean isDefault;
    private Boolean hasExtractedText;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
