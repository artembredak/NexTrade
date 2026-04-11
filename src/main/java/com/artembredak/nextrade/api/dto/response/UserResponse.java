package com.artembredak.nextrade.api.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String role
) {}