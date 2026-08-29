package com.hirehub.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hirehub.application.dto.*;
import com.hirehub.application.entity.Application;
import com.hirehub.application.repository.ApplicationRepository;
import com.hirehub.common.enums.ApplicationStage;
import com.hirehub.common.enums.NotificationType;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.DuplicateResourceException;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.company.entity.Company;
import com.hirehub.job.entity.Job;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.notification.entity.Notification;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApplicationResponse apply(Authentication auth, UUID jobId, CreateApplicationRequest request) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can apply for jobs");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        // Validate job is open
        if (job.getStatus() != com.hirehub.common.enums.JobStatus.ACTIVE) {
            throw new IllegalStateException("This job is no longer accepting applications");
        }

        // Validate deadline
        if (job.getApplicationDeadline().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Application deadline has passed");
        }

        // Check duplicate
        if (applicationRepository.existsByJobIdAndStudentId(jobId, student.getId())) {
            throw new DuplicateResourceException("You have already applied for this job");
        }

        // Create timeline event
        String timeline = createTimelineEvent("APPLIED", "Application submitted");

        Application application = Application.builder()
                .job(job)
                .student(student)
                .recruiter(job.getRecruiter())
                .company(job.getCompany())
                .status(ApplicationStage.APPLIED)
                .coverLetter(request.getCoverLetter())
                .timeline(timeline)
                .build();

        application = applicationRepository.save(application);

        // Increment application count
        job.setApplicationCount(job.getApplicationCount() + 1);
        jobRepository.save(job);

        // Notify the recruiter
        createNotification(
                job.getPostedBy(),
                "New Application",
                user.getName() + " applied for " + job.getTitle(),
                NotificationType.APPLICATION,
                "/dashboard/applicants?jobId=" + job.getId()
        );

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.findByStudentId(student.getId(), pageable)
                .map(a -> toResponse(a));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Authentication auth, UUID applicationId) {
        User user = findUserByEmail(auth.getName());
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        return switch (user.getRole()) {
            case ADMIN -> toResponse(app);
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student profile"));
                if (!app.getStudent().getId().equals(student.getId())) {
                    throw new ForbiddenException("You can only view your own applications");
                }
                yield toResponse(app);
            }
            case RECRUITER -> {
                Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
                if (recruiter.getCompany() == null ||
                    !recruiter.getCompany().getId().equals(app.getCompany().getId())) {
                    throw new ForbiddenException("You can only view applications for your company");
                }
                yield toResponse(app);
            }
        };
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getJobApplicants(Authentication auth, UUID jobId, int page, int size) {
        User user = findUserByEmail(auth.getName());
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        // Verify recruiter owns this job's company
        if (user.getRole() == Role.RECRUITER) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null ||
                !recruiter.getCompany().getId().equals(job.getCompany().getId())) {
                throw new ForbiddenException("You can only view applicants for your own company's jobs");
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.findByJobId(jobId, pageable)
                .map(a -> toResponse(a));
    }

    @Transactional
    public ApplicationResponse updateStatus(Authentication auth, UUID applicationId,
                                             UpdateApplicationStatusRequest request) {
        User user = findUserByEmail(auth.getName());
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        // Verify recruiter owns this application's company
        if (user.getRole() == Role.RECRUITER) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null ||
                !recruiter.getCompany().getId().equals(app.getCompany().getId())) {
                throw new ForbiddenException("You can only update applications for your company");
            }
        } else if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only recruiters and admins can update application status");
        }

        ApplicationStage oldStatus = app.getStatus();
        app.setStatus(request.getStatus());

        // Update timeline
        String timelineNote = request.getNote() != null ? request.getNote() :
                "Status changed to " + request.getStatus();
        app.setTimeline(addTimelineEvent(app.getTimeline(), request.getStatus().name(), timelineNote));

        app = applicationRepository.save(app);

        // Notify the student
        String title = "Application " + request.getStatus().name().toLowerCase().replace("_", " ");
        createNotification(
                app.getStudent().getUser(),
                title,
                "Your application for " + app.getJob().getTitle() + " has been " + request.getStatus().name().toLowerCase().replace("_", " "),
                NotificationType.APPLICATION,
                "/dashboard/applications"
        );

        return toResponse(app);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String createTimelineEvent(String stage, String note) {
        try {
            ArrayNode timeline = objectMapper.createArrayNode();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("stage", stage);
            event.put("note", note);
            event.put("timestamp", OffsetDateTime.now().toString());
            timeline.add(event);
            return objectMapper.writeValueAsString(timeline);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String addTimelineEvent(String existingTimeline, String stage, String note) {
        try {
            ArrayNode timeline;
            if (existingTimeline != null && !existingTimeline.isBlank()) {
                timeline = (ArrayNode) objectMapper.readTree(existingTimeline);
            } else {
                timeline = objectMapper.createArrayNode();
            }
            ObjectNode event = objectMapper.createObjectNode();
            event.put("stage", stage);
            event.put("note", note);
            event.put("timestamp", OffsetDateTime.now().toString());
            timeline.add(event);
            return objectMapper.writeValueAsString(timeline);
        } catch (Exception e) {
            return existingTimeline;
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

    public static ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob() != null ? app.getJob().getId() : null)
                .jobTitle(app.getJob() != null ? app.getJob().getTitle() : null)
                .studentId(app.getStudent() != null ? app.getStudent().getId() : null)
                .studentName(app.getStudent() != null && app.getStudent().getUser() != null ?
                        app.getStudent().getUser().getName() : null)
                .studentEmail(app.getStudent() != null && app.getStudent().getUser() != null ?
                        app.getStudent().getUser().getEmail() : null)
                .companyId(app.getCompany() != null ? app.getCompany().getId() : null)
                .companyName(app.getCompany() != null ? app.getCompany().getName() : null)
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .timeline(app.getTimeline())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.findAll(pageable).stream()
                .map(ApplicationService::toResponse)
                .toList();
    }
}
