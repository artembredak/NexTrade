package com.artembredak.nextrade.service;

import com.artembredak.nextrade.domain.model.Coin;
import com.artembredak.nextrade.domain.model.Portfolio;
import com.artembredak.nextrade.domain.model.PortfolioSnapshot;
import com.artembredak.nextrade.domain.model.PriceHistory;
import com.artembredak.nextrade.domain.repository.CoinRepository;
import com.artembredak.nextrade.domain.repository.PortfolioRepository;
import com.artembredak.nextrade.domain.repository.PortfolioSnapshotRepository;
import com.artembredak.nextrade.domain.repository.PriceHistoryRepository;
import com.artembredak.nextrade.infrastructure.client.CoinGeckoClient;
import com.artembredak.nextrade.infrastructure.client.CoinGeckoClient.CoinMarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceScheduler {

    private final CoinGeckoClient coinGeckoClient;
    private final CoinRepository coinRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;


    @Scheduled(fixedRateString = "${scheduler.price-update-interval-ms}")
    @Transactional
    public void updatePrices() {
        List<CoinMarketData> marketData = coinGeckoClient.fetchMarketData();

        if (marketData.isEmpty()) {
            log.warn("Price update skipped — CoinGecko returned no data");
            return;
        }

        int updatedCount = 0;

        for (CoinMarketData data : marketData) {
            try {
                upsertCoin(data);
                savePriceHistory(data);
                updatedCount++;
            } catch (Exception ex) {
                log.error("Failed to update price for coin '{}': {}", data.id(), ex.getMessage(), ex);
            }
        }

        log.info("Updated prices for {} coin(s)", updatedCount);
    }


    @Scheduled(fixedRateString = "${scheduler.snapshot-interval-ms}")
    @Transactional
    public void takePortfolioSnapshots() {
        List<Portfolio> portfolios = portfolioRepository.findAllWithAssetsAndCoins();

        OffsetDateTime now = OffsetDateTime.now();
        int snapshotCount = 0;

        for (Portfolio portfolio : portfolios) {
            try {
                BigDecimal cryptoValue = portfolio.getAssets().stream()
                        .filter(a -> a.getQuantity().compareTo(BigDecimal.ZERO) > 0
                                && a.getCoin().getCurrentPrice() != null)
                        .map(a -> a.getQuantity().multiply(a.getCoin().getCurrentPrice()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalValue = portfolio.getCashBalance().add(cryptoValue);

                PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                        .portfolio(portfolio)
                        .totalValue(totalValue)
                        .snapshotTime(now)
                        .build();

                portfolioSnapshotRepository.save(snapshot);
                snapshotCount++;

                log.debug("Snapshot recorded: portfolioId={}, totalValue={}",
                        portfolio.getId(), totalValue);
            } catch (Exception ex) {
                log.error("Failed to take snapshot for portfolioId='{}': {}",
                        portfolio.getId(), ex.getMessage(), ex);
            }
        }

        log.info("Took snapshots for {} portfolio(s)", snapshotCount);
    }


    private void upsertCoin(CoinMarketData data) {
        Coin coin = coinRepository.findById(data.id())
                .orElseGet(() -> Coin.builder().id(data.id()).build());

        coin.setSymbol(data.symbol());
        coin.setName(data.name());
        coin.setImageUrl(data.imageUrl());
        coin.setCurrentPrice(data.currentPrice());
        coin.setPriceChange24h(data.priceChange24h());
        coin.setMarketCap(data.marketCap());
        coin.setVolume24h(data.volume24h());
        coin.setLastUpdated(data.lastUpdated());

        coinRepository.saveAndFlush(coin);
    }


    private void savePriceHistory(CoinMarketData data) {
        Coin coinRef = coinRepository.getReferenceById(data.id());

        PriceHistory history = PriceHistory.builder()
                .coin(coinRef)
                .price(data.currentPrice())
                .recordedAt(OffsetDateTime.now())
                .build();

        priceHistoryRepository.save(history);
    }
}
