package com.artembredak.nextrade.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SellRequest(
        @NotBlank(message = "Coin ID is required")
        String coinId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity
) {}
