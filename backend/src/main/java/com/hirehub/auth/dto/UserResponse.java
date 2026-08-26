package com.hirehub.auth.dto;

import com.hirehub.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private String image;
    private Role role;
    private OffsetDateTime createdAt;
}
