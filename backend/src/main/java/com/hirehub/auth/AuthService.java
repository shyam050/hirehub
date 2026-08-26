package com.hirehub.auth;

import com.hirehub.auth.dto.*;
import com.hirehub.auth.entity.RefreshToken;
import com.hirehub.auth.repository.RefreshTokenRepository;
import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.DuplicateResourceException;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.UnauthorizedException;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Never allow ADMIN registration
        if (request.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Cannot register as admin through public registration");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .isAnonymous(false)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        // Create the role-specific profile
        if (request.getRole() == Role.STUDENT) {
            Student student = Student.builder()
                    .user(user)
                    .profileComplete(false)
                    .build();
            studentRepository.save(student);
        } else if (request.getRole() == Role.RECRUITER) {
            Recruiter recruiter = Recruiter.builder()
                    .user(user)
                    .build();
            recruiterRepository.save(recruiter);
        }

        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Account not found"));

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtService.hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isInvalid()) {
            // If token was used after revocation, revoke all tokens for this user (token reuse detected)
            if (storedToken.getRevoked()) {
                log.warn("Refresh token reuse detected for user {}, revoking all tokens", storedToken.getUser().getEmail());
                refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            }
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        User user = storedToken.getUser();

        // Rotate: revoke old token, issue new pair
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Account not found"));

        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("User logged out: {}", email);
    }

    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Account not found"));
        return toUserResponse(user);
    }

    // ── Internal helpers ──

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                toUserDetails(user),
                Map.of("role", user.getRole().name())
        );
        String refreshToken = jwtService.generateRefreshToken(toUserDetails(user));

        // Store hashed refresh token server-side
        RefreshToken storedRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(refreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(storedRefreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(toUserResponse(user))
                .build();
    }

    private UserDetails toUserDetails(User user) {
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "";
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                ))
        );
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .image(user.getImage())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
