package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId, Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.coin WHERE t.portfolio.id = :portfolioId ORDER BY t.createdAt DESC")
    Page<Transaction> findByPortfolioIdWithCoinOrderByCreatedAtDesc(@Param("portfolioId") UUID portfolioId, Pageable pageable);
}
