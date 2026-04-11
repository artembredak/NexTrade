package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, UUID> {

    List<PortfolioAsset> findByPortfolioId(UUID portfolioId);

    Optional<PortfolioAsset> findByPortfolioIdAndCoinId(UUID portfolioId, String coinId);

    @Modifying
    @Transactional
    void deleteByPortfolioIdAndCoinId(UUID portfolioId, String coinId);
}
