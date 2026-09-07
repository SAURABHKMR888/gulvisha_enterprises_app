package com.gulvisha.backend.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    Page<Project> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    List<Project> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<Project> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);
    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}
