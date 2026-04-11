package com.artembredak.nextrade.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CoinResponse(
        String id,
        String symbol,
        String name,
        String imageUrl,
        BigDecimal currentPrice,
        BigDecimal priceChange24h,
        BigDecimal marketCap,
        BigDecimal volume24h,
        OffsetDateTime lastUpdated
) {}
