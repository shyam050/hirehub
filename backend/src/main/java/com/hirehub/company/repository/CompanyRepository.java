package com.hirehub.company.repository;

import com.hirehub.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    long countByApproved(boolean approved);
}   