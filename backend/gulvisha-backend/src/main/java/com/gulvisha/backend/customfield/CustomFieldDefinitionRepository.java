package com.gulvisha.backend.customfield;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {
    List<CustomFieldDefinition> findAllByOrganizationIdAndEntityTypeOrderByCreatedAt(UUID organizationId, String entityType);
    List<CustomFieldDefinition> findAllByOrganizationIdAndEntityTypeAndEnabledTrue(UUID organizationId, String entityType);
}