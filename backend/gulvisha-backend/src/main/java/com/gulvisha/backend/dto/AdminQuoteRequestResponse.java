package com.gulvisha.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminQuoteRequestResponse(
        UUID id,
        String name,
        String email,
        String company,
        String service,
        String phone,
        String serviceCategory,
        String budget,
        String timeline,
        String source,
        String preferredContactMethod,
        String details,
        String status,
        String internalNotes,
        Instant receivedAt
) {
}