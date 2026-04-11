package com.artembredak.nextrade.api.dto.request;

import com.artembredak.nextrade.domain.model.SignalType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateSignalRequest(
        @NotBlank String coinId,
        @NotNull SignalType signalType,
        @NotNull @Min(0) @Max(100) Integer confidencePercent,
        @NotNull @Positive BigDecimal entryPrice,
        @NotNull @Positive BigDecimal targetPrice,
        @NotNull @Positive BigDecimal stopLossPrice,
        String rationale
) {}