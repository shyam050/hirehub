package com.hirehub.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralized custom metrics for HireHub.
 * All metric names use hirehub.* prefix.
 * No high-cardinality tags (no userId, email, resumeId, jobId).
 */
@Slf4j
@Service
public class MetricsService {

    private final Counter aiResumeAnalysisCounter;
    private final Counter aiJobMatchingCounter;
    private final Counter aiMockInterviewCounter;
    private final Counter aiFailureCounter;
    private final Counter authLoginCounter;
    private final Counter authOAuthCounter;
    private final Counter applicationSubmittedCounter;
    private final Timer aiRequestTimer;

    public MetricsService(MeterRegistry registry) {
        this.aiResumeAnalysisCounter = Counter.builder("hirehub.ai.resume.analysis")
                .description("Total resume analysis requests")
                .register(registry);
        this.aiJobMatchingCounter = Counter.builder("hirehub.ai.job.matching")
                .description("Total job matching requests")
                .register(registry);
        this.aiMockInterviewCounter = Counter.builder("hirehub.ai.mock.interview")
                .description("Total mock interview requests")
                .register(registry);
        this.aiFailureCounter = Counter.builder("hirehub.ai.failures")
                .description("Total AI service failures")
                .register(registry);
        this.authLoginCounter = Counter.builder("hirehub.auth.login")
                .description("Total login attempts")
                .register(registry);
        this.authOAuthCounter = Counter.builder("hirehub.auth.oauth")
                .description("Total OAuth authentication attempts")
                .register(registry);
        this.applicationSubmittedCounter = Counter.builder("hirehub.application.submitted")
                .description("Total job applications submitted")
                .register(registry);
        this.aiRequestTimer = Timer.builder("hirehub.ai.request.duration")
                .description("AI request duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordResumeAnalysis() { aiResumeAnalysisCounter.increment(); }
    public void recordJobMatching() { aiJobMatchingCounter.increment(); }
    public void recordMockInterview() { aiMockInterviewCounter.increment(); }
    public void recordAiFailure() { aiFailureCounter.increment(); }
    public void recordAuthLogin() { authLoginCounter.increment(); }
    public void recordAuthOAuth() { authOAuthCounter.increment(); }
    public void recordApplicationSubmitted() { applicationSubmittedCounter.increment(); }
    public Timer.Sample startAiTimer() { return Timer.start(); }
    public void recordAiTimer(Timer.Sample sample) { sample.stop(aiRequestTimer); }
}
