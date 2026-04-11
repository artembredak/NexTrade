package com.artembredak.nextrade.api.dto.response;

import com.artembredak.nextrade.domain.model.SignalStatus;
import com.artembredak.nextrade.domain.model.SignalType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SignalResponse(
        UUID id,
        String coinId,
        String coinName,
        String coinSymbol,
        SignalType signalType,
        int confidencePercent,
        BigDecimal entryPrice,
        BigDecimal targetPrice,
        BigDecimal stopLossPrice,
        String rationale,
        SignalStatus status,
        OffsetDateTime closedAt,
        OffsetDateTime createdAt
) {}
