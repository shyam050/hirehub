package com.hirehub.student.entity;

import com.hirehub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String phone;
    private String university;
    private String degree;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    private String gpa;
    private String bio;
    private String location;
    private String linkedin;
    private String github;
    private String portfolio;

    @Column(columnDefinition = "text")
    private String skills = "[]";

    @Column(columnDefinition = "text")
    private String education = "[]";

    @Column(columnDefinition = "text")
    private String projects = "[]";

    @Column(name = "profile_complete")
    private Boolean profileComplete = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.OffsetDateTime.now();
        updatedAt = java.time.OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.OffsetDateTime.now();
    }
}
