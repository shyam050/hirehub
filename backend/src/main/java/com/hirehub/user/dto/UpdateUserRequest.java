package com.hirehub.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 1, max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String image;
}
