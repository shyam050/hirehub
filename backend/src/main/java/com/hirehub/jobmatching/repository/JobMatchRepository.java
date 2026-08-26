package com.hirehub.jobmatching.repository;

import com.hirehub.jobmatching.entity.JobMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    Optional<JobMatch> findByStudentIdAndJobIdAndResumeId(UUID studentId, UUID jobId, UUID resumeId);

    @Query("SELECT jm FROM JobMatch jm WHERE jm.student.id = :studentId AND jm.job.id = :jobId AND jm.resume.id = :resumeId AND jm.createdAt >= :since ORDER BY jm.createdAt DESC LIMIT 1")
    Optional<JobMatch> findRecentMatch(@Param("studentId") UUID studentId,
                                        @Param("jobId") UUID jobId,
                                        @Param("resumeId") UUID resumeId,
                                        @Param("since") OffsetDateTime since);

    Page<JobMatch> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    List<JobMatch> findByStudentIdOrderByMatchScoreDesc(UUID studentId);

    List<JobMatch> findByResumeIdOrderByCreatedAtDesc(UUID resumeId);

    boolean existsByStudentIdAndJobIdAndResumeId(UUID studentId, UUID jobId, UUID resumeId);
}
