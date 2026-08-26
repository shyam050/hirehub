package com.hirehub.recruiter.repository;

import com.hirehub.recruiter.entity.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecruiterRepository extends JpaRepository<Recruiter, UUID> {
    Optional<Recruiter> findByUserId(UUID userId);
}
