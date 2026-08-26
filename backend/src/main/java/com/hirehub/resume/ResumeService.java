package com.hirehub.resume;

import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.resume.dto.ResumeResponse;
import com.hirehub.resume.entity.Resume;
import com.hirehub.resume.repository.ResumeRepository;
import com.hirehub.resume.storage.FileStorageService;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final String ALLOWED_EXTENSION = ".pdf";

    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /**
     * Upload a new resume for the authenticated student.
     */
    @Transactional
    public ResumeResponse uploadResume(MultipartFile file) {
        Student student = getAuthenticatedStudent();
        validateFile(file);

        String storageKey;
        try {
            storageKey = fileStorageService.store(
                    file.getInputStream(), file.getOriginalFilename(), ALLOWED_CONTENT_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store resume file");
        }

        Resume resume = Resume.builder()
                .student(student)
                .fileName(cleanFilename(file.getOriginalFilename()))
                .storageId(storageKey)
                .fileSize(file.getSize())
                .contentType(ALLOWED_CONTENT_TYPE)
                .isDefault(false)
                .build();

        // If this is the student's first resume, make it the default
        long existingCount = resumeRepository.countByStudentId(student.getId());
        if (existingCount == 0) {
            resume.setIsDefault(true);
        }

        resume = resumeRepository.save(resume);
        log.info("Resume uploaded: {} for student {}", resume.getId(), student.getId());

        return toResponse(resume);
    }

    /**
     * List all resumes for the authenticated student.
     */
    @Transactional(readOnly = true)
    public List<ResumeResponse> listMyResumes() {
        Student student = getAuthenticatedStudent();
        List<Resume> resumes = resumeRepository.findByStudentIdOrderByIsDefaultDescCreatedAtDesc(student.getId());
        return resumes.stream().map(this::toResponse).toList();
    }

    /**
     * Get a specific resume by ID. Only the owner or admin can access.
     */
    @Transactional(readOnly = true)
    public ResumeResponse getResume(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        authorizeAccess(resume);
        return toResponse(resume);
    }

    /**
     * Download a resume file. Only the owner or admin can access.
     */
    @Transactional(readOnly = true)
    public InputStream downloadResume(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        authorizeAccess(resume);
        return fileStorageService.retrieve(resume.getStorageId());
    }

    /**
     * Get the storage key for a resume (for content-type and filename info).
     */
    @Transactional(readOnly = true)
    public Resume getResumeEntity(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        authorizeAccess(resume);
        return resume;
    }

    /**
     * Delete a resume. Only the owner can delete.
     */
    @Transactional
    public void deleteResume(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        Student student = getAuthenticatedStudent();
        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only delete your own resumes");
        }

        boolean wasDefault = Boolean.TRUE.equals(resume.getIsDefault());

        // Remove the stored file
        fileStorageService.delete(resume.getStorageId());

        // Delete the database record
        resumeRepository.delete(resume);

        // If it was the default, set another resume as default
        if (wasDefault) {
            List<Resume> remaining = resumeRepository.findByStudentIdOrderByIsDefaultDescCreatedAtDesc(student.getId());
            if (!remaining.isEmpty()) {
                Resume newDefault = remaining.getFirst();
                newDefault.setIsDefault(true);
                resumeRepository.save(newDefault);
            }
        }

        log.info("Resume deleted: {} by student {}", resumeId, student.getId());
    }

    /**
     * Set a resume as the default. Only the owner can do this.
     */
    @Transactional
    public ResumeResponse setDefaultResume(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        Student student = getAuthenticatedStudent();
        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only manage your own resumes");
        }

        // Clear existing default
        resumeRepository.clearDefaultResume(student.getId());

        // Set new default
        resume.setIsDefault(true);
        resume = resumeRepository.save(resume);

        log.info("Default resume set: {} for student {}", resumeId, student.getId());
        return toResponse(resume);
    }

    // ── Authorization helpers ──

    private void authorizeAccess(Resume resume) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Authentication required");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        // Owner can access
        if (resume.getStudent().getUser().getId().equals(user.getId())) {
            return;
        }

        // Admin can access
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        throw new ForbiddenException("You do not have access to this resume");
    }

    private Student getAuthenticatedStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Authentication required");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can manage resumes");
        }

        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
    }

    // ── Validation helpers ──

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("Resume file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalStateException("File size exceeds maximum of 10 MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        boolean validContentType = ALLOWED_CONTENT_TYPE.equals(contentType);

        // Also check extension as fallback
        String originalFilename = file.getOriginalFilename();
        boolean validExtension = originalFilename != null
                && originalFilename.toLowerCase().endsWith(ALLOWED_EXTENSION);

        if (!validContentType && !validExtension) {
            throw new IllegalStateException("Only PDF files are accepted");
        }
    }

    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "resume.pdf";
        }
        // Remove path separators to prevent any path traversal in display
        return filename.replaceAll("[/\\\\]", "_");
    }

    // ── DTO mapping ──

    private ResumeResponse toResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .studentId(resume.getStudent().getId())
                .fileName(resume.getFileName())
                .fileSize(resume.getFileSize())
                .contentType(resume.getContentType())
                .isDefault(resume.getIsDefault())
                .hasExtractedText(resume.getExtractedText() != null && !resume.getExtractedText().isBlank())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }
}
