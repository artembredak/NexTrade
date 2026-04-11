package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.SignalStatus;
import com.artembredak.nextrade.domain.model.TradingSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradingSignalRepository extends JpaRepository<TradingSignal, UUID> {

    List<TradingSignal> findByStatusOrderByCreatedAtDesc(SignalStatus status);

    @Query("SELECT s FROM TradingSignal s JOIN FETCH s.coin WHERE s.status = :status ORDER BY s.createdAt DESC")
    List<TradingSignal> findByStatusWithCoinOrderByCreatedAtDesc(@Param("status") SignalStatus status);

    Optional<TradingSignal> findByIdAndStatus(UUID id, SignalStatus status);
}
