package com.hirehub.user.repository;

import com.hirehub.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hirehub.common.enums.Role;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByRole(Role role);
    long countByApproved(boolean approved);
}
