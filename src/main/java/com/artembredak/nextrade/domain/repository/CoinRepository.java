package com.artembredak.nextrade.domain.repository;

import com.artembredak.nextrade.domain.model.Coin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoinRepository extends JpaRepository<Coin, String> {

    List<Coin> findAllByOrderByMarketCapDesc();

    List<Coin> findByIdIn(List<String> ids);
}

