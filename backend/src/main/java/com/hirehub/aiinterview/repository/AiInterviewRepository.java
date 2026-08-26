package com.hirehub.aiinterview.repository;

import com.hirehub.aiinterview.entity.AiInterview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiInterviewRepository extends JpaRepository<AiInterview, UUID> {

    Page<AiInterview> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    long countByStudentIdAndStatusNot(UUID studentId, com.hirehub.common.enums.AiInterviewStatus status);
}
