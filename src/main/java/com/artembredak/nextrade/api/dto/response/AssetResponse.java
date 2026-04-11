package com.artembredak.nextrade.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetResponse(
        UUID assetId,
        String coinId,
        String coinName,
        String coinSymbol,
        String coinImageUrl,
        BigDecimal quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal investedValue,
        BigDecimal pnlUsd,
        BigDecimal pnlPercent,
        BigDecimal portfolioPercent
) {}
