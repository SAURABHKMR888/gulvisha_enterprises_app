package com.gulvisha.backend.repository;

import com.gulvisha.backend.entity.EnquiryAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnquiryAuditRepository extends JpaRepository<EnquiryAuditEntity, UUID> {
    List<EnquiryAuditEntity> findAllByEnquiryIdOrderByCreatedAtDesc(UUID enquiryId);
}