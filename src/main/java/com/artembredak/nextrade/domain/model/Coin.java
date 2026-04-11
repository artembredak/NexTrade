package com.artembredak.nextrade.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coins")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coin {

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "current_price", precision = 20, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "market_cap", precision = 30, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "volume_24h", precision = 30, scale = 2)
    private BigDecimal volume24h;

    @Column(name = "price_change_24h", precision = 10, scale = 4)
    private BigDecimal priceChange24h;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
