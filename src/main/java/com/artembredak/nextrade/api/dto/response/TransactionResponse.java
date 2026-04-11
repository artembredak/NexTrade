package com.artembredak.nextrade.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String type,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal pricePerCoin,
        String coinId,
        String coinSymbol,
        String description,
        OffsetDateTime createdAt
) {}