package com.gulvisha.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record QuoteRequestResponse(UUID id, Instant receivedAt, String message) {
}