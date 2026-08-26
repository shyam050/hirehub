package com.hirehub.student;

import com.hirehub.common.ApiResponse;
import com.hirehub.student.dto.StudentProfileResponse;
import com.hirehub.student.dto.UpdateStudentProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getMyProfile(auth.getName())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.updateMyProfile(auth.getName(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getStudentById(
            Authentication auth,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getStudentById(auth, id)));
    }
}
