package com.hirehub.jobmatching.entity;

import com.hirehub.job.entity.Job;
import com.hirehub.resume.entity.Resume;
import com.hirehub.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_matches",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "job_id", "resume_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore = 0;

    @Column(name = "matched_skills")
    private String matchedSkills = "[]";

    @Column(name = "missing_skills")
    private String missingSkills = "[]";

    @Column
    private String strengths = "[]";

    @Column
    private String recommendations = "[]";

    @Column(columnDefinition = "text")
    private String explanation;

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
