package com.gulvisha.backend.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String token,
        String username,
        List<String> roles,
        List<String> permissions,
        UUID clientId
) {
}
