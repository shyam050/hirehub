package com.hirehub.company;

import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.IllegalStateException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.company.dto.CompanyResponse;
import com.hirehub.company.dto.CreateCompanyRequest;
import com.hirehub.company.dto.UpdateCompanyRequest;
import com.hirehub.company.entity.Company;
import com.hirehub.company.repository.CompanyRepository;
import com.hirehub.recruiter.entity.Recruiter;
import com.hirehub.recruiter.repository.RecruiterRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;

    @Transactional
    public CompanyResponse createCompany(Authentication auth, CreateCompanyRequest request) {
        User user = findUserByEmail(auth.getName());

        if (user.getRole() != Role.RECRUITER) {
            throw new ForbiddenException("Only recruiters can create companies");
        }

        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile", "userId", user.getId().toString()));

        if (recruiter.getCompany() != null) {
            throw new IllegalStateException("You already have a company. Each recruiter can create one company.");
        }

        Company company = Company.builder()
                .name(request.getName())
                .description(request.getDescription())
                .industry(request.getIndustry())
                .size(request.getSize())
                .website(request.getWebsite())
                .logo(request.getLogo())
                .location(request.getLocation())
                .foundedYear(request.getFoundedYear())
                .approved(false)
                .createdBy(user)
                .build();

        company = companyRepository.save(company);

        // Link recruiter to the new company
        recruiter.setCompany(company);
        recruiterRepository.save(recruiter);

        return toResponse(company);
    }

    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id.toString()));
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse updateCompany(Authentication auth, UUID companyId, UpdateCompanyRequest request) {
        User user = findUserByEmail(auth.getName());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId.toString()));

        // Only the owner recruiter can update
        if (user.getRole() != Role.ADMIN) {
            Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
            if (recruiter.getCompany() == null || !recruiter.getCompany().getId().equals(companyId)) {
                throw new ForbiddenException("You can only update your own company");
            }
        }

        if (request.getName() != null) company.setName(request.getName());
        if (request.getDescription() != null) company.setDescription(request.getDescription());
        if (request.getIndustry() != null) company.setIndustry(request.getIndustry());
        if (request.getSize() != null) company.setSize(request.getSize());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if (request.getLogo() != null) company.setLogo(request.getLogo());
        if (request.getLocation() != null) company.setLocation(request.getLocation());
        if (request.getFoundedYear() != null) company.setFoundedYear(request.getFoundedYear());

        company = companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse approveCompany(Authentication auth, UUID companyId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can approve companies");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId.toString()));
        company.setApproved(true);
        company = companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse rejectCompany(Authentication auth, UUID companyId) {
        User user = findUserByEmail(auth.getName());
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can reject companies");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId.toString()));
        company.setApproved(false);
        company = companyRepository.save(company);
        return toResponse(company);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public static CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .size(company.getSize())
                .website(company.getWebsite())
                .logo(company.getLogo())
                .location(company.getLocation())
                .foundedYear(company.getFoundedYear())
                .approved(company.getApproved())
                .createdBy(company.getCreatedBy() != null ? company.getCreatedBy().getId() : null)
                .createdAt(company.getCreatedAt())
                .build();
    }
}
