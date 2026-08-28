package com.hirehub.user;

import com.hirehub.auth.AuthService;
import com.hirehub.auth.dto.UserResponse;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.user.dto.UpdateUserRequest;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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

    public Map<String, Object> getUserStats() {
        long totalUsers = userRepository.count();
        long studentCount = userRepository.count(); 
        long recruiterCount = 0; 

        return Map.of(
                "totalUsers", totalUsers,
                "students", studentCount,
                "recruiters", recruiterCount
        );
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}