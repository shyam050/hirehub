package com.hirehub.resumeanalysis.entity;

import com.hirehub.resume.entity.Resume;
import com.hirehub.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore = 0;

    @Column(name = "extracted_skills")
    private String extractedSkills = "[]";

    @Column(name = "extracted_education")
    private String extractedEducation = "[]";

    @Column(name = "extracted_projects")
    private String extractedProjects = "[]";

    @Column(name = "extracted_experience")
    private String extractedExperience = "[]";

    @Column(name = "extracted_certifications")
    private String extractedCertifications = "[]";

    @Column(name = "extracted_achievements")
    private String extractedAchievements = "[]";

    @Column
    private String strengths = "[]";

    @Column
    private String weaknesses = "[]";

    @Column(name = "missing_skills")
    private String missingSkills = "[]";

    @Column
    private String recommendations = "[]";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
