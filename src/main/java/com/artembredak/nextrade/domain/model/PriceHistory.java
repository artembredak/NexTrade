package com.artembredak.nextrade.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
