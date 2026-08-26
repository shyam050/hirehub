package com.hirehub.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hirehub.application.entity.Application;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.common.enums.InterviewStatus;
import com.hirehub.common.enums.InterviewType;
import com.hirehub.common.enums.NotificationType;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.interview.dto.*;
import com.hirehub.interview.entity.Interview;
import com.hirehub.interview.repository.InterviewRepository;
import com.hirehub.notification.entity.Notification;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    // ── Schedule Interview ──

    @Transactional
    public InterviewResponse scheduleInterview(Authentication auth, ScheduleInterviewRequest request) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER) {
            throw new ForbiddenException("Only recruiters can schedule interviews");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
        if (recruiter.getCompany() == null) {
            throw new ForbiddenException("You must have a company to schedule interviews");
        }

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId().toString()));

        // Verify recruiter owns this application's company
        if (!recruiter.getCompany().getId().equals(application.getCompany().getId())) {
            throw new ForbiddenException("You can only schedule interviews for your company's applications");
        }

        // Validate the scheduled time is in the future
        if (request.getScheduledAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Interview must be scheduled for the future");
        }

        Interview interview = Interview.builder()
                .application(application)
                .job(application.getJob())
                .student(application.getStudent())
                .company(application.getCompany())
                .recruiter(recruiter)
                .interviewType(request.getInterviewType())
                .scheduledAt(request.getScheduledAt())
                .duration(request.getDuration())
                .meetingLink(request.getMeetingLink())
                .interviewerName(request.getInterviewerName())
                .notes(request.getNotes())
                .status(InterviewStatus.SCHEDULED)
                .build();

        interview = interviewRepository.save(interview);

        // Create timeline event on application
        addTimelineEvent(application, "INTERVIEW_SCHEDULED",
                "Interview scheduled: " + request.getInterviewType() + " on " + request.getScheduledAt());
        applicationRepository.save(application);

        // Notify student
        createNotification(
                application.getStudent().getUser(),
                "Interview Scheduled",
                "Your interview for " + application.getJob().getTitle() + " has been scheduled for " + request.getScheduledAt(),
                NotificationType.INTERVIEW,
                "/dashboard/interviews"
        );

        log.info("Interview scheduled: {} for application {}", interview.getId(), application.getId());
        return toResponse(interview);
    }

    // ── Get Student Interviews ──

    @Transactional(readOnly = true)
    public Page<InterviewResponse> getMyInterviews(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can view their interviews");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        return interviewRepository.findByStudentIdOrderByScheduledAtDesc(student.getId(), pageable)
                .map(this::toResponse);
    }

    // ── Get Interview by ID ──

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(Authentication auth, UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId.toString()));

        authorizeInterviewAccess(user, interview);
        return toResponse(interview);
    }

    // ── Get Application Interviews ──

    @Transactional(readOnly = true)
    public List<InterviewResponse> getApplicationInterviews(Authentication auth, UUID applicationId) {
        User user = findUserByEmail(auth.getName());
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        // Verify access
        switch (user.getRole()) {
            case ADMIN -> { /* full access */ }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student profile"));
                if (!application.getStudent().getId().equals(student.getId())) {
                    throw new ForbiddenException("You can only view interviews for your own applications");
                }
            }
            case RECRUITER -> {
                Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
                if (recruiter.getCompany() == null ||
                    !recruiter.getCompany().getId().equals(application.getCompany().getId())) {
                    throw new ForbiddenException("You can only view interviews for your company's applications");
                }
            }
        }

        return interviewRepository.findByApplicationIdOrderByScheduledAtDesc(applicationId)
                .stream().map(this::toResponse).toList();
    }

    // ── Get Recruiter Interviews ──

    @Transactional(readOnly = true)
    public Page<InterviewResponse> getRecruiterInterviews(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only recruiters can view their company interviews");
        }

        if (user.getRole() == Role.RECRUITER) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null) {
                throw new ForbiddenException("You must have a company to view interviews");
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
            return interviewRepository.findByCompanyIdOrderByScheduledAtDesc(recruiter.getCompany().getId(), pageable)
                    .map(this::toResponse);
        }

        // Admin: return all interviews
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        return interviewRepository.findAll(pageable).map(this::toResponse);
    }

    // ── Reschedule Interview ──

    @Transactional
    public InterviewResponse rescheduleInterview(Authentication auth, UUID interviewId,
                                                  RescheduleInterviewRequest request) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER) {
            throw new ForbiddenException("Only recruiters can reschedule interviews");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
        if (recruiter.getCompany() == null) {
            throw new ForbiddenException("You must have a company");
        }

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId.toString()));

        // Verify recruiter owns this interview's company
        if (!recruiter.getCompany().getId().equals(interview.getCompany().getId())) {
            throw new ForbiddenException("You can only reschedule interviews for your company");
        }

        // Cannot reschedule a completed interview
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reschedule a completed interview");
        }

        // Validate new time
        if (request.getScheduledAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("New interview time must be in the future");
        }

        interview.setScheduledAt(request.getScheduledAt());
        if (request.getDuration() != null) interview.setDuration(request.getDuration());
        if (request.getMeetingLink() != null) interview.setMeetingLink(request.getMeetingLink());
        if (request.getInterviewerName() != null) interview.setInterviewerName(request.getInterviewerName());
        interview.setStatus(InterviewStatus.RESCHEDULED);

        interview = interviewRepository.save(interview);

        // Timeline event
        addTimelineEvent(interview.getApplication(), "INTERVIEW_RESCHEDULED",
                "Interview rescheduled to " + request.getScheduledAt());
        applicationRepository.save(interview.getApplication());

        // Notify student
        createNotification(
                interview.getStudent().getUser(),
                "Interview Rescheduled",
                "Your interview for " + interview.getJob().getTitle() + " has been rescheduled to " + request.getScheduledAt(),
                NotificationType.INTERVIEW,
                "/dashboard/interviews"
        );

        return toResponse(interview);
    }

    // ── Cancel Interview ──

    @Transactional
    public InterviewResponse cancelInterview(Authentication auth, UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER) {
            throw new ForbiddenException("Only recruiters can cancel interviews");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
        if (recruiter.getCompany() == null) {
            throw new ForbiddenException("You must have a company");
        }

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId.toString()));

        if (!recruiter.getCompany().getId().equals(interview.getCompany().getId())) {
            throw new ForbiddenException("You can only cancel interviews for your company");
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed interview");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview = interviewRepository.save(interview);

        // Timeline event
        addTimelineEvent(interview.getApplication(), "INTERVIEW_CANCELLED",
                "Interview cancelled");
        applicationRepository.save(interview.getApplication());

        // Notify student
        createNotification(
                interview.getStudent().getUser(),
                "Interview Cancelled",
                "Your interview for " + interview.getJob().getTitle() + " has been cancelled",
                NotificationType.INTERVIEW,
                "/dashboard/interviews"
        );

        return toResponse(interview);
    }

    // ── Complete Interview ──

    @Transactional
    public InterviewResponse completeInterview(Authentication auth, UUID interviewId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER) {
            throw new ForbiddenException("Only recruiters can complete interviews");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
        if (recruiter.getCompany() == null) {
            throw new ForbiddenException("You must have a company");
        }

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId.toString()));

        if (!recruiter.getCompany().getId().equals(interview.getCompany().getId())) {
            throw new ForbiddenException("You can only complete interviews for your company");
        }

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Cannot complete a cancelled interview");
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Interview is already completed");
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview = interviewRepository.save(interview);

        // Timeline event
        addTimelineEvent(interview.getApplication(), "INTERVIEW_COMPLETED",
                "Interview completed");
        applicationRepository.save(interview.getApplication());

        // Notify student
        createNotification(
                interview.getStudent().getUser(),
                "Interview Completed",
                "Your interview for " + interview.getJob().getTitle() + " has been completed",
                NotificationType.INTERVIEW,
                "/dashboard/interviews"
        );

        return toResponse(interview);
    }

    // ── Submit Feedback ──

    @Transactional
    public InterviewResponse submitFeedback(Authentication auth, UUID interviewId,
                                             SubmitFeedbackRequest request) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER) {
            throw new ForbiddenException("Only recruiters can submit feedback");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
        if (recruiter.getCompany() == null) {
            throw new ForbiddenException("You must have a company");
        }

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId.toString()));

        if (!recruiter.getCompany().getId().equals(interview.getCompany().getId())) {
            throw new ForbiddenException("You can only submit feedback for your company's interviews");
        }

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Feedback can only be submitted for completed interviews");
        }

        interview.setFeedback(request.getFeedback());
        if (request.getNotes() != null) {
            interview.setNotes(request.getNotes());
        }
        interview = interviewRepository.save(interview);

        // Timeline event
        addTimelineEvent(interview.getApplication(), "INTERVIEW_FEEDBACK",
                "Interview feedback submitted");
        applicationRepository.save(interview.getApplication());

        // Notify student
        createNotification(
                interview.getStudent().getUser(),
                "Interview Feedback Available",
                "Feedback for your " + interview.getInterviewType() + " interview for " +
                        interview.getJob().getTitle() + " is now available",
                NotificationType.INTERVIEW,
                "/dashboard/interviews"
        );

        return toResponse(interview);
    }

    // ── Authorization ──

    private void authorizeInterviewAccess(User user, Interview interview) {
        switch (user.getRole()) {
            case ADMIN -> { /* full access */ }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student profile"));
                if (!interview.getStudent().getId().equals(student.getId())) {
                    throw new ForbiddenException("You can only view your own interviews");
                }
            }
            case RECRUITER -> {
                Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
                if (recruiter.getCompany() == null ||
                    !recruiter.getCompany().getId().equals(interview.getCompany().getId())) {
                    throw new ForbiddenException("You can only view interviews for your company");
                }
            }
        }
    }

    // ── Helpers ──

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void addTimelineEvent(Application application, String stage, String note) {
        try {
            ArrayNode timeline;
            String existing = application.getTimeline();
            if (existing != null && !existing.isBlank()) {
                timeline = (ArrayNode) objectMapper.readTree(existing);
            } else {
                timeline = objectMapper.createArrayNode();
            }
            ObjectNode event = objectMapper.createObjectNode();
            event.put("stage", stage);
            event.put("note", note);
            event.put("timestamp", OffsetDateTime.now().toString());
            timeline.add(event);
            application.setTimeline(objectMapper.writeValueAsString(timeline));
        } catch (Exception e) {
            log.error("Failed to add timeline event", e);
        }
    }

    private void createNotification(User user, String title, String message,
                                     NotificationType type, String link) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    private InterviewResponse toResponse(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplication() != null ? interview.getApplication().getId() : null)
                .jobId(interview.getJob() != null ? interview.getJob().getId() : null)
                .jobTitle(interview.getJob() != null ? interview.getJob().getTitle() : null)
                .companyId(interview.getCompany() != null ? interview.getCompany().getId() : null)
                .companyName(interview.getCompany() != null ? interview.getCompany().getName() : null)
                .studentId(interview.getStudent() != null ? interview.getStudent().getId() : null)
                .studentName(interview.getStudent() != null && interview.getStudent().getUser() != null ?
                        interview.getStudent().getUser().getName() : null)
                .studentEmail(interview.getStudent() != null && interview.getStudent().getUser() != null ?
                        interview.getStudent().getUser().getEmail() : null)
                .interviewType(interview.getInterviewType())
                .scheduledAt(interview.getScheduledAt())
                .duration(interview.getDuration())
                .meetingLink(interview.getMeetingLink())
                .interviewerName(interview.getInterviewerName())
                .status(interview.getStatus())
                .notes(interview.getNotes())
                .feedback(interview.getFeedback())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }
}
