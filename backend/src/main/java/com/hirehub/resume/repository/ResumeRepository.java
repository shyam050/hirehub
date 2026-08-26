package com.hirehub.resume.repository;

import com.hirehub.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByStudentIdOrderByIsDefaultDescCreatedAtDesc(UUID studentId);

    Optional<Resume> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<Resume> findById(UUID id);

    @Query("SELECT r FROM Resume r WHERE r.student.id = :studentId AND r.isDefault = true")
    Optional<Resume> findDefaultByStudentId(UUID studentId);

    long countByStudentId(UUID studentId);

    @Modifying
    @Transactional
    @Query("UPDATE Resume r SET r.isDefault = false WHERE r.student.id = :studentId AND r.isDefault = true")
    int clearDefaultResume(UUID studentId);

    void deleteByStudentId(UUID studentId);
}
