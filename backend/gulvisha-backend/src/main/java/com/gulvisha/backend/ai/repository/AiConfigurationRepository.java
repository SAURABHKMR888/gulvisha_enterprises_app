package com.gulvisha.backend.ai.repository;

import com.gulvisha.backend.ai.entity.AiConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiConfigurationRepository extends JpaRepository<AiConfiguration, UUID> {
    Optional<AiConfiguration> findByOrganizationId(UUID organizationId);
}
