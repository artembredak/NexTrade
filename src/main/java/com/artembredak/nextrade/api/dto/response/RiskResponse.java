package com.artembredak.nextrade.api.dto.response;

import java.math.BigDecimal;

public record RiskResponse(
        int riskScore,
        String riskLevel,
        BigDecimal concentrationRisk,
        BigDecimal volatilityRisk,
        BigDecimal cashPercent,
        String recommendation
) {}