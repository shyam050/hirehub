package com.hirehub.resumeanalysis;

import com.hirehub.common.RateLimiter;
import com.hirehub.resumeanalysis.dto.ResumeAnalysisResponse;
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
public class ResumeAnalysisController {

    private final ResumeAnalysisService analysisService;
    private final RateLimiter rateLimiter;

    @PostMapping("/resumes/{resumeId}/analyze")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            Authentication auth,
            @PathVariable UUID resumeId,
            HttpServletRequest request) {
        rateLimiter.checkAiRateLimit("analyze:" + auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(analysisService.analyzeResume(auth, resumeId));
    }

    @GetMapping("/resumes/{resumeId}/analyses")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<ResumeAnalysisResponse>> getAnalysesForResume(
            Authentication auth,
            @PathVariable UUID resumeId) {
        return ResponseEntity.ok(analysisService.getAnalysesForResume(auth, resumeId));
    }

    @GetMapping("/resumes/{resumeId}/analyses/latest")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ResumeAnalysisResponse> getLatestAnalysis(
            Authentication auth,
            @PathVariable UUID resumeId) {
        return ResponseEntity.ok(analysisService.getLatestAnalysis(auth, resumeId));
    }

    @GetMapping("/resume-analyses/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ResumeAnalysisResponse>> getMyAnalyses(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analysisService.getMyAnalyses(auth, page, size));
    }
}
