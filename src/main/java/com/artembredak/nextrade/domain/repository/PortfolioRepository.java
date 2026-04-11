package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    List<Portfolio> findByUserId(UUID userId);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.assets WHERE p.user.id = :userId")
    List<Portfolio> findByUserIdWithAssets(@Param("userId") UUID userId);


    @Query("""
            SELECT p FROM Portfolio p
            LEFT JOIN FETCH p.assets a
            LEFT JOIN FETCH a.coin
            WHERE p.user.id = :userId
            """)
    List<Portfolio> findByUserIdWithAssetsAndCoins(@Param("userId") UUID userId);

    Optional<Portfolio> findByUserIdAndId(UUID userId, UUID id);


    @Query("""
            SELECT DISTINCT p FROM Portfolio p
            LEFT JOIN FETCH p.assets a
            LEFT JOIN FETCH a.coin
            """)
    List<Portfolio> findAllWithAssetsAndCoins();
}
