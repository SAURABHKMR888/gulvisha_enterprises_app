package com.gulvisha.backend.crm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
    Page<Lead> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    List<Lead> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<Lead> findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, String status);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
    List<Lead> findAllBySourceIdContaining(UUID organizationId, String sourceId, Pageable pageable);
    Optional<Lead> findByOrganizationIdAndSourceId(UUID organizationId, String sourceId);
    
    @Modifying
    @Query("UPDATE Lead l SET l.status = :status WHERE l.id = :id AND l.organizationId = :orgId")
    void updateStatus(@Param("id") UUID id, @Param("orgId") UUID orgId, @Param("status") String status);
}