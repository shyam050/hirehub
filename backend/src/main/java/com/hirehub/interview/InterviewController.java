package com.hirehub.interview;

import com.hirehub.interview.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            Authentication auth,
            @Valid @RequestBody ScheduleInterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.scheduleInterview(auth, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<InterviewResponse>> getMyInterviews(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(interviewService.getMyInterviews(auth, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<InterviewResponse> getInterview(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterview(auth, id));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<List<InterviewResponse>> getApplicationInterviews(
            Authentication auth,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(interviewService.getApplicationInterviews(auth, applicationId));
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Page<InterviewResponse>> getRecruiterInterviews(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(interviewService.getRecruiterInterviews(auth, page, size));
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> rescheduleInterview(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleInterviewRequest request) {
        return ResponseEntity.ok(interviewService.rescheduleInterview(auth, id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> cancelInterview(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.cancelInterview(auth, id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> completeInterview(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.completeInterview(auth, id));
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewResponse> submitFeedback(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        return ResponseEntity.ok(interviewService.submitFeedback(auth, id, request));
    }
}
