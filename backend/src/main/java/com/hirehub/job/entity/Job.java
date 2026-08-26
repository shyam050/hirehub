package com.hirehub.job.entity;

import com.hirehub.common.enums.JobStatus;
import com.hirehub.common.enums.JobType;
import com.hirehub.company.entity.Company;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by", nullable = false)
    private User postedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Column(columnDefinition = "text")
    private String requirements = "[]";

    @Column(columnDefinition = "text")
    private String skills = "[]";

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType = JobType.FULL_TIME;

    private Boolean remote = false;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Column(name = "experience_min")
    private BigDecimal experienceMin;

    @Column(name = "experience_max")
    private BigDecimal experienceMax;

    @Column(name = "education_required")
    private String educationRequired;

    @Column(name = "application_deadline", nullable = false)
    private OffsetDateTime applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "application_count")
    private Integer applicationCount = 0;

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
