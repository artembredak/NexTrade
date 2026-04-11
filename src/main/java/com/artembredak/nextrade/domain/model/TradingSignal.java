package com.artembredak.nextrade.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trading_signals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 10)
    private SignalType signalType;

    @Column(name = "confidence_percent", nullable = false)
    private int confidencePercent;

    @Column(name = "entry_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "target_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal targetPrice;

    @Column(name = "stop_loss_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal stopLossPrice;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private SignalStatus status = SignalStatus.ACTIVE;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
