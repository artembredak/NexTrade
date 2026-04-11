package com.artembredak.nextrade.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioSummaryResponse(
        UUID portfolioId,
        String name,
        BigDecimal totalValue,
        BigDecimal cashBalance,
        BigDecimal cryptoValue,
        BigDecimal cashPercent,
        BigDecimal cryptoPercent,
        BigDecimal change24hPercent,
        int assetCount
) {}