package com.hirehub.interview.repository;

import com.hirehub.interview.entity.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    Page<Interview> findByStudentIdOrderByScheduledAtDesc(UUID studentId, Pageable pageable);

    List<Interview> findByApplicationIdOrderByScheduledAtDesc(UUID applicationId);

    Page<Interview> findByCompanyIdOrderByScheduledAtDesc(UUID companyId, Pageable pageable);

    boolean existsByApplicationIdAndStatusNot(UUID applicationId, com.hirehub.common.enums.InterviewStatus status);
}
