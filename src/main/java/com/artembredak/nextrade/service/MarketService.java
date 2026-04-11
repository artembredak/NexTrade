package com.artembredak.nextrade.service;

import com.artembredak.nextrade.api.dto.response.CoinResponse;
import com.artembredak.nextrade.api.exception.NotFoundException;
import com.artembredak.nextrade.domain.model.Coin;
import com.artembredak.nextrade.domain.repository.CoinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MarketService {

    private final CoinRepository coinRepository;


    public List<CoinResponse> getAllCoins() {
        List<CoinResponse> coins = coinRepository.findAllByOrderByMarketCapDesc()
                .stream()
                .map(this::toCoinResponse)
                .toList();

        log.debug("getAllCoins: returning {} coin(s)", coins.size());
        return coins;
    }

    public CoinResponse getCoinById(String coinId) {
        Coin coin = coinRepository.findById(coinId)
                .orElseThrow(() -> new NotFoundException("Coin", coinId));

        log.debug("getCoinById: found coinId={}", coinId);
        return toCoinResponse(coin);
    }

    // -------------------------------------------------------------------------
    // Private mapper
    // -------------------------------------------------------------------------

    private CoinResponse toCoinResponse(Coin coin) {
        return new CoinResponse(
                coin.getId(),
                coin.getSymbol(),
                coin.getName(),
                coin.getImageUrl(),
                coin.getCurrentPrice(),
                coin.getPriceChange24h(),
                coin.getMarketCap(),
                coin.getVolume24h(),
                coin.getLastUpdated()
        );
    }
}
