package com.hirehub.application.repository;

import com.hirehub.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    boolean existsByJobIdAndStudentId(UUID jobId, UUID studentId);
    Page<Application> findByStudentId(UUID studentId, Pageable pageable);
    Page<Application> findByJobId(UUID jobId, Pageable pageable);
}
