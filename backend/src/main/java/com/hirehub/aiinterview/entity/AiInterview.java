package com.hirehub.aiinterview.entity;

import com.hirehub.common.enums.*;
import com.hirehub.job.entity.Job;
import com.hirehub.resume.entity.Resume;
import com.hirehub.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInterview {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false)
    private AiInterviewType interviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiInterviewDifficulty difficulty = AiInterviewDifficulty.MEDIUM;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 5;

    @Column(name = "current_question_number", nullable = false)
    private Integer currentQuestionNumber = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiInterviewStatus status = AiInterviewStatus.NOT_STARTED;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "technical_score")
    private Integer technicalScore;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "problem_solving_score")
    private Integer problemSolvingScore;

    @Column
    private String strengths = "[]";

    @Column
    private String weaknesses = "[]";

    @Column(name = "missing_concepts")
    private String missingConcepts = "[]";

    @Column(name = "recommended_topics")
    private String recommendedTopics = "[]";

    @Column(name = "overall_feedback", columnDefinition = "text")
    private String overallFeedback;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

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
