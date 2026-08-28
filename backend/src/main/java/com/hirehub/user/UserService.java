package com.hirehub.user;

import com.hirehub.auth.AuthService;
import com.hirehub.auth.dto.UserResponse;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.user.dto.UpdateUserRequest;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.job.repository.JobRepository;
import com.hirehub.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public UserResponse getMe(String email) {
        User user = findByEmail(email);
        return AuthService.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateMe(String email, UpdateUserRequest request) {
        User user = findByEmail(email);

        if (request.getName() != null) user.setName(request.getName());
        if (request.getImage() != null) user.setImage(request.getImage());

        user = userRepository.save(user);
        return AuthService.toUserResponse(user);
    }

    public UserResponse getUserById(java.util.UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        return AuthService.toUserResponse(user);
    }

    // ── Admin endpoints ──

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalRecruiters = userRepository.countByRole(Role.RECRUITER);
        long totalCompanies = companyRepository.count();
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();
        long pendingCompanies = companyRepository.countByApproved(false);

        return Map.of(
                "totalStudents", totalStudents,
                "totalRecruiters", totalRecruiters,
                "totalCompanies", totalCompanies,
                "totalJobs", totalJobs,
                "totalApplications", totalApplications,
                "pendingCompanies", pendingCompanies
        );
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllStudents() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .map(AuthService::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllRecruiters() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.RECRUITER)
                .map(AuthService::toUserResponse)
                .collect(Collectors.toList());
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}