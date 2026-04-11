package com.artembredak.nextrade.service;

import com.artembredak.nextrade.api.dto.response.AssetResponse;
import com.artembredak.nextrade.api.dto.response.PortfolioChartResponse;
import com.artembredak.nextrade.api.dto.response.PortfolioSummaryResponse;
import com.artembredak.nextrade.api.exception.BusinessException;
import com.artembredak.nextrade.api.exception.NotFoundException;
import com.artembredak.nextrade.domain.model.*;
import com.artembredak.nextrade.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PortfolioService {

    private static final int SCALE_INTERMEDIATE = 8;
    private static final int SCALE_PERCENT = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final CoinRepository coinRepository;


    public PortfolioSummaryResponse getSummary(String userEmail) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        BigDecimal cryptoValue = computeCryptoValue(portfolio);
        BigDecimal totalValue = portfolio.getCashBalance().add(cryptoValue);

        BigDecimal change24hPercent = compute24hChangePercent(portfolio.getId(), totalValue);

        BigDecimal cashPercent = BigDecimal.ZERO;
        BigDecimal cryptoPercent = BigDecimal.ZERO;
        if (totalValue.compareTo(BigDecimal.ZERO) != 0) {
            cashPercent = portfolio.getCashBalance()
                    .divide(totalValue, SCALE_INTERMEDIATE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE_PERCENT, ROUNDING);
            cryptoPercent = cryptoValue
                    .divide(totalValue, SCALE_INTERMEDIATE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE_PERCENT, ROUNDING);
        }

        long activeAssets = portfolio.getAssets().stream()
                .filter(a -> a.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .count();

        log.debug("Portfolio summary computed: portfolioId={}, totalValue={}, assetCount={}",
                portfolio.getId(), totalValue, activeAssets);

        return new PortfolioSummaryResponse(
                portfolio.getId(),
                portfolio.getName(),
                totalValue.setScale(SCALE_INTERMEDIATE, ROUNDING),
                portfolio.getCashBalance(),
                cryptoValue.setScale(SCALE_INTERMEDIATE, ROUNDING),
                cashPercent,
                cryptoPercent,
                change24hPercent,
                (int) activeAssets
        );
    }

    /**
     * Returns full P&amp;L detail for every asset in the authenticated user's portfolio
     * that has a positive quantity. Results are sorted by current value descending.
     */
    public List<AssetResponse> getAssets(String userEmail) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        BigDecimal totalValue = portfolio.getCashBalance().add(computeCryptoValue(portfolio));

        return portfolio.getAssets().stream()
                .filter(a -> a.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(asset -> toAssetResponse(asset, totalValue))
                .sorted(Comparator.comparing(AssetResponse::currentValue).reversed())
                .toList();
    }


    public PortfolioChartResponse getChart(String userEmail, String period) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);
        OffsetDateTime cutoff = resolveCutoff(period);

        List<PortfolioChartResponse.ChartPoint> points = portfolioSnapshotRepository
                .findByPortfolioIdAndSnapshotTimeAfterOrderBySnapshotTimeAsc(
                        portfolio.getId(), cutoff)
                .stream()
                .map(s -> new PortfolioChartResponse.ChartPoint(s.getSnapshotTime(), s.getTotalValue()))
                .toList();

        log.debug("Chart data loaded: portfolioId={}, period={}, pointCount={}",
                portfolio.getId(), period, points.size());

        return new PortfolioChartResponse(period, points);
    }


    @Transactional
    void recordSnapshot(UUID portfolioId, BigDecimal totalValue) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .portfolio(portfolio)
                .totalValue(totalValue)
                .snapshotTime(OffsetDateTime.now())
                .build();

        portfolioSnapshotRepository.save(snapshot);
        log.info("Portfolio snapshot recorded: portfolioId={}, totalValue={}", portfolioId, totalValue);
    }


    @Transactional
    public void updateAssetAfterBuy(UUID portfolioId, String coinId,
                                    BigDecimal quantity, BigDecimal buyPrice) {
        portfolioAssetRepository.findByPortfolioIdAndCoinId(portfolioId, coinId)
                .ifPresentOrElse(
                        existing -> {
                            BigDecimal oldQty = existing.getQuantity();
                            BigDecimal newQty = oldQty.add(quantity);
                            BigDecimal newAvg = oldQty.multiply(existing.getAvgBuyPrice())
                                    .add(quantity.multiply(buyPrice))
                                    .divide(newQty, SCALE_INTERMEDIATE, ROUNDING);
                            existing.setQuantity(newQty);
                            existing.setAvgBuyPrice(newAvg);
                            portfolioAssetRepository.save(existing);
                            log.info("Asset updated after buy: portfolioId={}, coinId={}, " +
                                            "newQty={}, newAvgBuyPrice={}",
                                    portfolioId, coinId, newQty, newAvg);
                        },
                        () -> {
                            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                                    .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

                            Coin coin = coinRepository.findById(coinId)
                                    .orElseThrow(() -> new NotFoundException("Coin", coinId));

                            PortfolioAsset asset = PortfolioAsset.builder()
                                    .portfolio(portfolio)
                                    .coin(coin)
                                    .quantity(quantity)
                                    .avgBuyPrice(buyPrice)
                                    .build();
                            portfolioAssetRepository.save(asset);
                            log.info("New asset created after buy: portfolioId={}, coinId={}, " +
                                    "qty={}, avgBuyPrice={}", portfolioId, coinId, quantity, buyPrice);
                        }
                );
    }

    @Transactional
    public void updateAssetAfterSell(UUID portfolioId, String coinId, BigDecimal quantity) {
        PortfolioAsset asset = portfolioAssetRepository
                .findByPortfolioIdAndCoinId(portfolioId, coinId)
                .orElseThrow(() -> new NotFoundException(
                        "Asset not found for portfolioId=" + portfolioId + ", coinId=" + coinId));

        if (quantity.compareTo(asset.getQuantity()) > 0) {
            throw new BusinessException(
                    "Insufficient quantity: available=" + asset.getQuantity() +
                            ", requested=" + quantity,
                    HttpStatus.BAD_REQUEST);
        }

        BigDecimal remaining = asset.getQuantity().subtract(quantity);

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            portfolioAssetRepository.delete(asset);
            log.info("Asset deleted after full sell: portfolioId={}, coinId={}", portfolioId, coinId);
        } else {
            asset.setQuantity(remaining);
            portfolioAssetRepository.save(asset);
            log.info("Asset reduced after sell: portfolioId={}, coinId={}, remaining={}",
                    portfolioId, coinId, remaining);
        }
    }

    private Portfolio getPortfolioByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        return portfolioRepository.findByUserIdWithAssetsAndCoins(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Portfolio not found for user: " + email));
    }

    /**
     * Sums quantity * currentPrice across all assets that have a positive quantity
     * and a non-null currentPrice.
     */
    private BigDecimal computeCryptoValue(Portfolio portfolio) {
        return portfolio.getAssets().stream()
                .filter(a -> a.getQuantity().compareTo(BigDecimal.ZERO) > 0
                        && a.getCoin().getCurrentPrice() != null)
                .map(a -> a.getQuantity().multiply(a.getCoin().getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Finds the most recent snapshot older than 24 h and computes the percentage change
     * relative to the current total value. Returns null when no such snapshot exists.
     */
    private BigDecimal compute24hChangePercent(UUID portfolioId, BigDecimal currentTotalValue) {
        OffsetDateTime cutoff24h = OffsetDateTime.now().minusHours(24);

        return portfolioSnapshotRepository
                .findTopByPortfolioIdAndSnapshotTimeBeforeOrderBySnapshotTimeDesc(
                        portfolioId, cutoff24h)
                .map(snapshot -> {
                    BigDecimal value24hAgo = snapshot.getTotalValue();
                    if (value24hAgo.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                    }
                    return currentTotalValue.subtract(value24hAgo)
                            .divide(value24hAgo, SCALE_INTERMEDIATE, ROUNDING)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(SCALE_PERCENT, ROUNDING);
                })
                .orElse(null);
    }

    /**
     * Maps a PortfolioAsset to an AssetResponse with all P&amp;L fields populated.
     * Uses ZERO as the currentPrice when the coin price has not been loaded yet.
     */
    private AssetResponse toAssetResponse(PortfolioAsset asset, BigDecimal totalPortfolioValue) {
        BigDecimal currentPrice = asset.getCoin().getCurrentPrice() != null
                ? asset.getCoin().getCurrentPrice()
                : BigDecimal.ZERO;

        BigDecimal currentValue = asset.getQuantity()
                .multiply(currentPrice)
                .setScale(SCALE_INTERMEDIATE, ROUNDING);

        BigDecimal investedValue = asset.getQuantity()
                .multiply(asset.getAvgBuyPrice())
                .setScale(SCALE_INTERMEDIATE, ROUNDING);

        BigDecimal pnlUsd = currentValue.subtract(investedValue);

        BigDecimal pnlPercent;
        if (asset.getAvgBuyPrice().compareTo(BigDecimal.ZERO) == 0) {
            pnlPercent = BigDecimal.ZERO;
        } else {
            pnlPercent = currentPrice.subtract(asset.getAvgBuyPrice())
                    .divide(asset.getAvgBuyPrice(), SCALE_INTERMEDIATE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE_PERCENT, ROUNDING);
        }

        BigDecimal portfolioPercent = BigDecimal.ZERO;
        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) != 0) {
            portfolioPercent = currentValue
                    .divide(totalPortfolioValue, SCALE_INTERMEDIATE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE_PERCENT, ROUNDING);
        }

        return new AssetResponse(
                asset.getId(),
                asset.getCoin().getId(),
                asset.getCoin().getName(),
                asset.getCoin().getSymbol(),
                asset.getCoin().getImageUrl(),
                asset.getQuantity(),
                asset.getAvgBuyPrice(),
                currentPrice,
                currentValue,
                investedValue,
                pnlUsd,
                pnlPercent,
                portfolioPercent
        );
    }


    private OffsetDateTime resolveCutoff(String period) {
        OffsetDateTime now = OffsetDateTime.now();
        return switch (period) {
            case "1d" -> now.minusDays(1);
            case "1w" -> now.minusDays(7);
            case "1m" -> now.minusDays(30);
            case "3m" -> now.minusDays(90);
            default -> throw new BusinessException(
                    "Invalid period '" + period + "'. Accepted values: 1d, 1w, 1m, 3m",
                    HttpStatus.BAD_REQUEST);
        };
    }
}
