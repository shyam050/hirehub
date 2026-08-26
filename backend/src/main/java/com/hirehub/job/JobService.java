package com.hirehub.job;

import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.common.enums.JobStatus;
import com.hirehub.company.entity.Company;
import com.hirehub.job.dto.*;
import com.hirehub.job.entity.Job;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;

    @Transactional
    public JobResponse createJob(Authentication auth, CreateJobRequest request) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.RECRUITER && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only recruiters can create jobs");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
        Company company = recruiter.getCompany();
        if (company == null) {
            throw new IllegalStateException("You must create a company before posting jobs");
        }

        Job job = Job.builder()
                .company(company)
                .postedBy(user)
                .recruiter(recruiter)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .jobType(request.getJobType())
                .remote(request.getRemote() != null ? request.getRemote() : false)
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .experienceMin(request.getExperienceMin())
                .experienceMax(request.getExperienceMax())
                .educationRequired(request.getEducationRequired())
                .requirements(request.getRequirements())
                .skills(request.getSkills())
                .applicationDeadline(request.getApplicationDeadline())
                .status(JobStatus.ACTIVE)
                .applicationCount(0)
                .build();

        job = jobRepository.save(job);
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(String search, String location, String type,
                                         String status, int page, int size) {
        Specification<Job> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("skills")), pattern)
                ));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")),
                        "%" + location.toLowerCase() + "%"));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("jobType"),
                        com.hirehub.common.enums.JobType.valueOf(type.toUpperCase())));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"),
                        JobStatus.valueOf(status.toUpperCase())));
            } else {
                // Default: only active jobs for public browsing
                predicates.add(cb.equal(root.get("status"), JobStatus.ACTIVE));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobRepository.findAll(spec, pageable).map(j -> toResponse(j));
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobsForRecruiter(Authentication auth, String search,
                                                      String status, int page, int size) {
        User user = findUserByEmail(auth.getName());
        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));

        Specification<Job> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("company"), recruiter.getCompany()));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"),
                        JobStatus.valueOf(status.toUpperCase())));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobRepository.findAll(spec, pageable).map(j -> toResponse(j));
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id.toString()));
        return toResponse(job);
    }

    @Transactional
    public JobResponse updateJob(Authentication auth, UUID jobId, UpdateJobRequest request) {
        User user = findUserByEmail(auth.getName());
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        // Verify ownership
        if (user.getRole() != Role.ADMIN) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null ||
                !recruiter.getCompany().getId().equals(job.getCompany().getId())) {
                throw new ForbiddenException("You can only update your own company's jobs");
            }
        }

        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getJobType() != null) job.setJobType(request.getJobType());
        if (request.getRemote() != null) job.setRemote(request.getRemote());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getExperienceMin() != null) job.setExperienceMin(request.getExperienceMin());
        if (request.getExperienceMax() != null) job.setExperienceMax(request.getExperienceMax());
        if (request.getEducationRequired() != null) job.setEducationRequired(request.getEducationRequired());
        if (request.getRequirements() != null) job.setRequirements(request.getRequirements());
        if (request.getSkills() != null) job.setSkills(request.getSkills());
        if (request.getApplicationDeadline() != null) job.setApplicationDeadline(request.getApplicationDeadline());
        if (request.getStatus() != null && user.getRole() == Role.ADMIN) job.setStatus(request.getStatus());

        job = jobRepository.save(job);
        return toResponse(job);
    }

    @Transactional
    public void deleteJob(Authentication auth, UUID jobId) {
        User user = findUserByEmail(auth.getName());
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (user.getRole() != Role.ADMIN) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null ||
                !recruiter.getCompany().getId().equals(job.getCompany().getId())) {
                throw new ForbiddenException("You can only delete your own company's jobs");
            }
        }

        jobRepository.delete(job);
    }

    @Transactional
    public JobResponse closeJob(Authentication auth, UUID jobId) {
        User user = findUserByEmail(auth.getName());
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (user.getRole() != Role.ADMIN) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null ||
                !recruiter.getCompany().getId().equals(job.getCompany().getId())) {
                throw new ForbiddenException("You can only close your own company's jobs");
            }
        }

        job.setStatus(JobStatus.CLOSED);
        job = jobRepository.save(job);
        return toResponse(job);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public static JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .postedBy(job.getPostedBy() != null ? job.getPostedBy().getId() : null)
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .remote(job.getRemote())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .experienceMin(job.getExperienceMin())
                .experienceMax(job.getExperienceMax())
                .educationRequired(job.getEducationRequired())
                .requirements(job.getRequirements())
                .skills(job.getSkills())
                .applicationDeadline(job.getApplicationDeadline())
                .status(job.getStatus())
                .applicationCount(job.getApplicationCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
