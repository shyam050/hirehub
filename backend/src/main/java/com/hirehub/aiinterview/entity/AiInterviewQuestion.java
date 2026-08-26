package com.hirehub.aiinterview.entity;

import com.hirehub.common.enums.QuestionCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_interview_questions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"interview_id", "question_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private AiInterview interview;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(columnDefinition = "text", nullable = false)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionCategory category;

    @Column(name = "expected_topics", columnDefinition = "jsonb")
    private String expectedTopics = "[]";

    @Column(name = "student_answer", columnDefinition = "text")
    private String studentAnswer;

    @Column
    private Integer score;

    @Column(name = "strengths", columnDefinition = "jsonb")
    private String strengths = "[]";


    @Column(name = "weaknesses", columnDefinition = "jsonb")
    private String weaknesses = "[]";


    @Column(columnDefinition = "text")
    private String feedback;

    @Column(name = "missing_concepts", columnDefinition = "jsonb")
    private String missingConcepts = "[]";

    @Column(name = "ideal_answer_points", columnDefinition = "jsonb")
    private String idealAnswerPoints = "[]";

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
