package com.hirehub.student.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UpdateStudentProfileRequest {

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 255, message = "University must not exceed 255 characters")
    private String university;

    @Size(max = 255, message = "Degree must not exceed 255 characters")
    private String degree;

    @Size(max = 255, message = "Field of study must not exceed 255 characters")
    private String fieldOfStudy;

    private Integer graduationYear;

    @Size(max = 20, message = "GPA must not exceed 20 characters")
    private String gpa;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 500, message = "LinkedIn URL must not exceed 500 characters")
    private String linkedin;

    @Size(max = 500, message = "GitHub URL must not exceed 500 characters")
    private String github;

    @Size(max = 500, message = "Portfolio URL must not exceed 500 characters")
    private String portfolio;

    private List<String> skills;
    private List<String> education;
    private List<String> projects;
}