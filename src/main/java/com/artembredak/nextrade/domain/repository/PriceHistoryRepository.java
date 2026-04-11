package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByCoinIdAndRecordedAtAfterOrderByRecordedAtAsc(String coinId, OffsetDateTime after);

    Optional<PriceHistory> findTopByCoinIdOrderByRecordedAtDesc(String coinId);
}
