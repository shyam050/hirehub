package com.hirehub.auth.repository;

import com.hirehub.auth.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthProviderRepository extends JpaRepository<OAuthProvider, UUID> {

    Optional<OAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<OAuthProvider> findByProviderAndEmail(String provider, String email);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUserId(UUID userId);
}
