package com.hirehub.resume;

import com.hirehub.resume.dto.ResumeResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestParam("file") MultipartFile file) {
        ResumeResponse response = resumeService.uploadResume(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ResumeResponse>> listMyResumes() {
        return ResponseEntity.ok(resumeService.listMyResumes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ResumeResponse> getResume(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeService.getResume(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public void downloadResume(@PathVariable UUID id, HttpServletResponse response) throws IOException {
        com.hirehub.resume.entity.Resume resume = resumeService.getResumeEntity(id);

        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resume.getFileName() + "\"");
        response.setContentLengthLong(resume.getFileSize());

        try (InputStream is = resumeService.downloadResume(id)) {
            is.transferTo(response.getOutputStream());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteResume(@PathVariable UUID id) {
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResumeResponse> setDefaultResume(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeService.setDefaultResume(id));
    }
}
