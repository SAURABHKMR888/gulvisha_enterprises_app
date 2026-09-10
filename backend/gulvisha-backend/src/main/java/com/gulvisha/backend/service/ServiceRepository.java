package com.gulvisha.backend.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findAllByOrganizationIdOrderByDisplayOrderAscNameAsc(UUID organizationId);
    List<Service> findAllByOrganizationIdAndPublicVisibleTrueOrderByDisplayOrderAsc(UUID organizationId);
    List<Service> findAllByOrganizationIdAndStatusOrderByDisplayOrderAsc(UUID organizationId, String status);
}
