package com.gulvisha.backend.pipeline;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {
    List<PipelineStage> findAllByOrganizationIdOrderBySortOrder(UUID organizationId);
}