package com.hirehub.recruiter;

import com.hirehub.common.ApiResponse;
import com.hirehub.recruiter.dto.RecruiterProfileResponse;
import com.hirehub.recruiter.dto.UpdateRecruiterProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterService recruiterService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(recruiterService.getMyProfile(auth.getName())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateRecruiterProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recruiterService.updateMyProfile(auth.getName(), request)));
    }
}
