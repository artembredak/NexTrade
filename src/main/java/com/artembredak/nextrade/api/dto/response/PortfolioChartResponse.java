package com.artembredak.nextrade.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioChartResponse(
        String period,
        List<ChartPoint> points
) {

    public record ChartPoint(
            OffsetDateTime timestamp,
            BigDecimal value
    ) {}
}