package com.gulvisha.backend.repository;

import com.gulvisha.backend.entity.QuoteRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequestEntity, UUID> {
    Page<QuoteRequestEntity> findAllByOrganizationIdOrderByReceivedAtDesc(UUID organizationId, Pageable pageable);
    Page<QuoteRequestEntity> findAllByOrganizationIdAndStatusOrderByReceivedAtDesc(UUID organizationId, String status, Pageable pageable);

        @Query("""
                        SELECT request FROM QuoteRequestEntity request
                        WHERE request.organizationId = :organizationId
                            AND (:status IS NULL OR request.status = :status)
                            AND (
                                LOWER(request.name) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(request.email) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(COALESCE(request.company, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(request.service) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(request.details) LIKE LOWER(CONCAT('%', :search, '%'))
                            )
                        ORDER BY request.receivedAt DESC
                        """)
        Page<QuoteRequestEntity> search(
                        @Param("organizationId") UUID organizationId,
                        @Param("status") String status,
                        @Param("search") String search,
                        Pageable pageable
        );

        @Query("""
                        SELECT request FROM QuoteRequestEntity request
                        WHERE request.organizationId = :organizationId
                            AND (:status IS NULL OR request.status = :status)
                            AND (:search IS NULL OR LOWER(request.name) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(request.email) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(COALESCE(request.company, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(request.service) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(request.details) LIKE LOWER(CONCAT('%', :search, '%')))
                            AND (:fromDate IS NULL OR request.receivedAt >= :fromDate)
                            AND (:toDate IS NULL OR request.receivedAt < :toDate)
                        ORDER BY request.receivedAt DESC
                        """)
        Page<QuoteRequestEntity> filter(
                        @Param("organizationId") UUID organizationId,
                        @Param("status") String status,
                        @Param("search") String search,
                        @Param("fromDate") Instant fromDate,
                        @Param("toDate") Instant toDate,
                        Pageable pageable
        );

    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}