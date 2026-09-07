package com.gulvisha.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditResponse(
        UUID id,
        UUID enquiryId,
        String action,
        String previousValue,
        String newValue,
        String actor,
        Instant createdAt
) {
}