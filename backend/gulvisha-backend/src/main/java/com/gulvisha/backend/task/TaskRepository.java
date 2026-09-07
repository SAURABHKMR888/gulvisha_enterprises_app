package com.gulvisha.backend.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    List<Task> findAllByOrganizationIdAndProjectIdOrderByCreatedAtDesc(UUID organizationId, UUID projectId);
    List<Task> findAllByOrganizationIdAndProjectIdInOrderByCreatedAtDesc(UUID organizationId, Collection<UUID> projectIds);
    List<Task> findAllByOrganizationIdAndAssignedToOrderByCreatedAtDesc(UUID organizationId, String assignedTo);
    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}
