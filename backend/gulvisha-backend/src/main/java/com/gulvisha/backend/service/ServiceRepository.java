package com.gulvisha.backend.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    List<Service> findAllByOrganizationIdAndStatusOrderByNameAsc(UUID organizationId, String status);
}
