package com.hirehub.application;

import com.hirehub.application.dto.*;
import com.hirehub.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/api/v1/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            Authentication auth,
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(applicationService.apply(auth, jobId, request)));
    }

    @GetMapping("/api/v1/applications/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getMyApplications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getMyApplications(auth, page, size)));
    }

    @GetMapping("/api/v1/applications/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplication(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getApplicationById(auth, id)));
    }

    @GetMapping("/api/v1/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getJobApplicants(
            Authentication auth,
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getJobApplicants(auth, jobId, page, size)));
    }

    @PatchMapping("/api/v1/applications/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.updateStatus(auth, id, request)));
    }
}
