package com.hirehub.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 1, max = 255, message = "Company name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 255, message = "Industry must not exceed 255 characters")
    private String industry;

    @Size(max = 100, message = "Size must not exceed 100 characters")
    private String size;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String website;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logo;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    private Integer foundedYear;
}
