package com.hirehub.aiinterview;

import com.hirehub.aiinterview.dto.*;
import com.hirehub.common.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/ai-interviews")
@RequiredArgsConstructor
public class AiInterviewController {

    private final AiInterviewService interviewService;
    private final RateLimiter rateLimiter;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AiInterviewResponse> startInterview(
            Authentication auth,
            @Valid @RequestBody StartInterviewRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAiRateLimit("ai-interview:" + auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.startInterview(auth, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<AiInterviewResponse>> getMyInterviews(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(interviewService.getMyInterviews(auth, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<AiInterviewResponse> getInterview(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterview(auth, id));
    }

    @GetMapping("/{id}/questions")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<AiInterviewQuestionResponse>> getInterviewQuestions(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterviewQuestions(auth, id));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<AiInterviewResponse> getInterviewReport(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterviewReport(auth, id));
    }

    @PostMapping("/{id}/next-question")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AiInterviewQuestionResponse> getNextQuestion(
            Authentication auth,
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAiRateLimit("ai-interview:" + auth.getName());
        return ResponseEntity.ok(interviewService.getNextQuestion(auth, id));
    }

    @PostMapping("/{id}/questions/{questionNumber}/answer")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AnswerEvaluationResponse> submitAnswer(
            Authentication auth,
            @PathVariable UUID id,
            @PathVariable int questionNumber,
            @Valid @RequestBody SubmitAnswerRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAiRateLimit("ai-interview:" + auth.getName());
        return ResponseEntity.ok(interviewService.submitAnswer(auth, id, questionNumber, request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AiInterviewResponse> completeInterview(
            Authentication auth,
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        rateLimiter.checkAiRateLimit("ai-interview:" + auth.getName());
        return ResponseEntity.ok(interviewService.completeInterview(auth, id));
    }

    @PostMapping("/{id}/abandon")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AiInterviewResponse> abandonInterview(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.abandonInterview(auth, id));
    }
}
