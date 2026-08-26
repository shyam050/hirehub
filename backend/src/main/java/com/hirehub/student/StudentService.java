package com.hirehub.student;

import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.student.dto.StudentProfileResponse;
import com.hirehub.student.dto.UpdateStudentProfileRequest;
import com.hirehub.student.entity.Student;
import com.hirehub.student.repository.StudentRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentProfileResponse getMyProfile(String email) {
        User user = findUserByEmail(email);
        Student student = findStudentByUserId(user.getId());
        return toResponse(student, user);
    }

    @Transactional
    public StudentProfileResponse updateMyProfile(String email, UpdateStudentProfileRequest request) {
        User user = findUserByEmail(email);
        Student student = findStudentByUserId(user.getId());

        if (request.getPhone() != null) student.setPhone(request.getPhone());
        if (request.getUniversity() != null) student.setUniversity(request.getUniversity());
        if (request.getDegree() != null) student.setDegree(request.getDegree());
        if (request.getFieldOfStudy() != null) student.setFieldOfStudy(request.getFieldOfStudy());
        if (request.getGraduationYear() != null) student.setGraduationYear(request.getGraduationYear());
        if (request.getGpa() != null) student.setGpa(request.getGpa());
        if (request.getBio() != null) student.setBio(request.getBio());
        if (request.getLocation() != null) student.setLocation(request.getLocation());
        if (request.getLinkedin() != null) student.setLinkedin(request.getLinkedin());
        if (request.getGithub() != null) student.setGithub(request.getGithub());
        if (request.getPortfolio() != null) student.setPortfolio(request.getPortfolio());
        if (request.getSkills() != null) student.setSkills(request.getSkills()); // JSON string
        if (request.getEducation() != null) student.setEducation(request.getEducation());
        if (request.getProjects() != null) student.setProjects(request.getProjects());

        // Update profile completion
        boolean complete = student.getUniversity() != null && !student.getUniversity().isBlank()
                && student.getDegree() != null && !student.getDegree().isBlank()
                && student.getSkills() != null && !student.getSkills().isBlank();
        student.setProfileComplete(complete);

        student = studentRepository.save(student);
        return toResponse(student, user);
    }

    /**
     * Access a student profile by ID. Enforces access rules:
     * - Students can only access their own profile
     * - Recruiters can access profiles of students who applied to their company (future V3.5)
     * - Admins can access any profile
     */
    public StudentProfileResponse getStudentById(Authentication auth, java.util.UUID studentId) {
        User caller = findUserByEmail(auth.getName());
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId.toString()));
        User studentUser = student.getUser();

        return switch (caller.getRole()) {
            case ADMIN -> toResponse(student, studentUser);
            case STUDENT -> {
                if (!caller.getId().equals(studentUser.getId())) {
                    throw new ForbiddenException("You can only access your own profile");
                }
                yield toResponse(student, studentUser);
            }
            case RECRUITER -> {
                // V3.5 will add application-based access. For now, deny.
                throw new ForbiddenException("Recruiters cannot access student profiles directly");
            }
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Student findStudentByUserId(java.util.UUID userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile", "userId", userId.toString()));
    }

    public static StudentProfileResponse toResponse(Student student, User user) {
        return StudentProfileResponse.builder()
                .id(student.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(student.getPhone())
                .university(student.getUniversity())
                .degree(student.getDegree())
                .fieldOfStudy(student.getFieldOfStudy())
                .graduationYear(student.getGraduationYear())
                .gpa(student.getGpa())
                .bio(student.getBio())
                .location(student.getLocation())
                .linkedin(student.getLinkedin())
                .github(student.getGithub())
                .portfolio(student.getPortfolio())
                .skills(student.getSkills())
                .education(student.getEducation())
                .projects(student.getProjects())
                .profileComplete(student.getProfileComplete())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
