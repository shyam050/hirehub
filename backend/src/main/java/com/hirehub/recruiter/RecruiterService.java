package com.hirehub.recruiter;

import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.recruiter.dto.RecruiterProfileResponse;
import com.hirehub.recruiter.dto.UpdateRecruiterProfileRequest;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;

    public RecruiterProfileResponse getMyProfile(String email) {
        User user = findUserByEmail(email);

        if (user.getRole() == Role.ADMIN) {
            return RecruiterProfileResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .jobTitle("Administrator")
                    .build();
        }

        Recruiter recruiter = findRecruiterByUserId(user.getId());
        return toResponse(recruiter, user);
    }

    @Transactional
    public RecruiterProfileResponse updateMyProfile(String email, UpdateRecruiterProfileRequest request) {
        User user = findUserByEmail(email);

        if (user.getRole() == Role.ADMIN) {
            return RecruiterProfileResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .jobTitle("Administrator")
                    .build();
        }

        Recruiter recruiter = findRecruiterByUserId(user.getId());

        if (request.getJobTitle() != null) recruiter.setJobTitle(request.getJobTitle());
        if (request.getPhone() != null) recruiter.setPhone(request.getPhone());
        if (request.getBio() != null) recruiter.setBio(request.getBio());

        recruiter = recruiterRepository.save(recruiter);
        return toResponse(recruiter, user);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Recruiter findRecruiterByUserId(java.util.UUID userId) {
        return recruiterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile", "userId", userId.toString()));
    }

    public static RecruiterProfileResponse toResponse(Recruiter recruiter, User user) {
        return RecruiterProfileResponse.builder()
                .id(recruiter.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .jobTitle(recruiter.getJobTitle())
                .phone(recruiter.getPhone())
                .bio(recruiter.getBio())
                .companyId(recruiter.getCompany() != null ? recruiter.getCompany().getId() : null)
                .companyName(recruiter.getCompany() != null ? recruiter.getCompany().getName() : null)
                .createdAt(recruiter.getCreatedAt())
                .build();
    }
}