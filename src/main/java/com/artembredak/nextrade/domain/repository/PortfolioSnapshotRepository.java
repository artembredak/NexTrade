package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByPortfolioIdAndSnapshotTimeAfterOrderBySnapshotTimeAsc(
            UUID portfolioId, OffsetDateTime after);

    Optional<PortfolioSnapshot> findTopByPortfolioIdOrderBySnapshotTimeDesc(UUID portfolioId);

    /**
     * Returns the most recent snapshot recorded strictly before the given cutoff time.
     * Used to retrieve the 24 h-ago reference value for change_24h_percent calculation.
     */
    Optional<PortfolioSnapshot> findTopByPortfolioIdAndSnapshotTimeBeforeOrderBySnapshotTimeDesc(
            UUID portfolioId, OffsetDateTime before);
}
