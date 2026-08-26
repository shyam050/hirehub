package com.hirehub.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class CompanyResponse {
    private UUID id;
    private String name;
    private String description;
    private String industry;
    private String size;
    private String website;
    private String logo;
    private String location;
    private Integer foundedYear;
    private Boolean approved;
    private UUID createdBy;
    private OffsetDateTime createdAt;
}
