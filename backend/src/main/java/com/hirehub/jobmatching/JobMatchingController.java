package com.hirehub.jobmatching;

import com.hirehub.common.RateLimiter;
import com.hirehub.jobmatching.dto.JobMatchResponse;
import com.hirehub.jobmatching.dto.SkillGapResponse;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JobMatchingController {

    private final JobMatchingService matchingService;
    private final RateLimiter rateLimiter;

    @PostMapping("/jobs/{jobId}/match")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<JobMatchResponse> calculateMatch(
            Authentication auth,
            @PathVariable UUID jobId,
            @RequestParam(required = false) UUID resumeId,
            HttpServletRequest request) {
        rateLimiter.checkAiRateLimit("match:" + auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchingService.calculateMatch(auth, jobId, resumeId));
    }

    @GetMapping("/jobs/{jobId}/match")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<JobMatchResponse> getMatchForJob(
            Authentication auth,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(matchingService.getMatchForJob(auth, jobId));
    }

    @GetMapping("/job-matches/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<JobMatchResponse>> getMyMatches(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(matchingService.getMyMatches(auth, page, size));
    }

    @GetMapping("/job-matches/me/skill-gaps")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SkillGapResponse>> getSkillGaps(Authentication auth) {
        return ResponseEntity.ok(matchingService.getSkillGaps(auth));
    }

    @GetMapping("/resumes/{resumeId}/matches")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<JobMatchResponse>> getMatchesForResume(
            Authentication auth,
            @PathVariable UUID resumeId) {
        return ResponseEntity.ok(matchingService.getMatchesForResume(auth, resumeId));
    }
}
