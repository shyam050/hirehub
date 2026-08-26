package com.hirehub.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class StudentProfileResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private String university;
    private String degree;
    private String fieldOfStudy;
    private Integer graduationYear;
    private String gpa;
    private String bio;
    private String location;
    private String linkedin;
    private String github;
    private String portfolio;
    private String skills;
    private String education;
    private String projects;
    private Boolean profileComplete;
    private OffsetDateTime createdAt;
}
