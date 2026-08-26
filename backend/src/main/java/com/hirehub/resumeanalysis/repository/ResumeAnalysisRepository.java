package com.hirehub.resumeanalysis.repository;

import com.hirehub.resumeanalysis.entity.ResumeAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {

    Page<ResumeAnalysis> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    List<ResumeAnalysis> findByResumeIdOrderByCreatedAtDesc(UUID resumeId);

    Optional<ResumeAnalysis> findFirstByResumeIdOrderByCreatedAtDesc(UUID resumeId);

    @Query("SELECT ra FROM ResumeAnalysis ra WHERE ra.resume.id = :resumeId AND ra.createdAt >= :since ORDER BY ra.createdAt DESC LIMIT 1")
    Optional<ResumeAnalysis> findRecentByResumeId(@Param("resumeId") UUID resumeId, @Param("since") OffsetDateTime since);

    boolean existsByStudentIdAndResumeId(UUID studentId, UUID resumeId);
}
