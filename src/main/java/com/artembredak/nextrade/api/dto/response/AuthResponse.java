package com.artembredak.nextrade.api.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessExpiresIn,
        UserResponse user
) {}
