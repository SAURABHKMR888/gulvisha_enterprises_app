package com.gulvisha.backend.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID>, JpaSpecificationExecutor<Resource> {
    List<Resource> findAllByOrganizationIdOrderByName(UUID organizationId);
    List<Resource> findAllByOrganizationIdAndStatus(UUID organizationId, String status);
    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}
