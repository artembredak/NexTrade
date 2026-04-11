package com.artembredak.nextrade.service;

import com.artembredak.nextrade.api.dto.response.RiskResponse;
import com.artembredak.nextrade.api.exception.NotFoundException;
import com.artembredak.nextrade.domain.model.Portfolio;
import com.artembredak.nextrade.domain.model.PortfolioAsset;
import com.artembredak.nextrade.domain.model.User;
import com.artembredak.nextrade.domain.repository.PortfolioRepository;
import com.artembredak.nextrade.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RiskService {

    private static final int SCALE = 8;
    private static final int SCALE_PERCENT = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;


    public RiskResponse calculateRisk(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found: " + userEmail));


        List<Portfolio> portfolios = portfolioRepository.findByUserIdWithAssetsAndCoins(user.getId());

        if (portfolios.isEmpty()) {
            log.debug("No portfolio found for user={}, returning zero risk", userEmail);
            return zeroRisk();
        }

        Portfolio portfolio = portfolios.getFirst();
        List<PortfolioAsset> activeAssets = portfolio.getAssets().stream()
                .filter(a -> a.getQuantity().compareTo(BigDecimal.ZERO) > 0
                        && a.getCoin().getCurrentPrice() != null)
                .toList();

        if (activeAssets.isEmpty()) {
            log.debug("Portfolio has no active assets for user={}, returning zero risk", userEmail);
            return zeroRisk();
        }


        BigDecimal cryptoValue = activeAssets.stream()
                .map(a -> a.getQuantity().multiply(a.getCoin().getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashBalance = portfolio.getCashBalance();
        BigDecimal totalValue = cashBalance.add(cryptoValue);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return zeroRisk();
        }


        BigDecimal largestAssetValue = activeAssets.stream()
                .map(a -> a.getQuantity().multiply(a.getCoin().getCurrentPrice()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal concentrationRiskPct = largestAssetValue
                .divide(totalValue, SCALE, ROUNDING)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE_PERCENT, ROUNDING);

        int concentrationPoints;
        if (concentrationRiskPct.compareTo(BigDecimal.valueOf(80)) > 0) {
            concentrationPoints = 40;
        } else if (concentrationRiskPct.compareTo(BigDecimal.valueOf(60)) > 0) {
            concentrationPoints = 25;
        } else if (concentrationRiskPct.compareTo(BigDecimal.valueOf(40)) > 0) {
            concentrationPoints = 15;
        } else {
            concentrationPoints = 0;
        }


        BigDecimal sumAbsChange = activeAssets.stream()
                .filter(a -> a.getCoin().getPriceChange24h() != null)
                .map(a -> a.getCoin().getPriceChange24h().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long coinsWithChange = activeAssets.stream()
                .filter(a -> a.getCoin().getPriceChange24h() != null)
                .count();

        BigDecimal avgAbsChange = coinsWithChange > 0
                ? sumAbsChange.divide(BigDecimal.valueOf(coinsWithChange), SCALE, ROUNDING)
                : BigDecimal.ZERO;

        int volatilityPoints = avgAbsChange.compareTo(BigDecimal.TEN) > 0 ? 30 : 0;
        BigDecimal volatilityRiskPts = BigDecimal.valueOf(volatilityPoints);


        BigDecimal cashPercent = cashBalance
                .divide(totalValue, SCALE, ROUNDING)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE_PERCENT, ROUNDING);

        int liquidityPoints;
        if (cashPercent.compareTo(BigDecimal.valueOf(5)) < 0) {
            liquidityPoints = 20;
        } else if (cashPercent.compareTo(BigDecimal.valueOf(30)) > 0) {
            liquidityPoints = -10;
        } else {
            liquidityPoints = 0;
        }


        int rawScore = concentrationPoints + volatilityRiskPts.intValue() + liquidityPoints;
        int riskScore = Math.min(100, Math.max(0, rawScore));

        String riskLevel = resolveRiskLevel(riskScore);
        String recommendation = resolveRecommendation(riskLevel);

        log.debug("Risk calculated for user={}: score={}, level={}, concentrationPct={}, " +
                        "volatilityPts={}, cashPct={}",
                userEmail, riskScore, riskLevel, concentrationRiskPct,
                volatilityRiskPts.setScale(SCALE_PERCENT, ROUNDING), cashPercent);

        return new RiskResponse(
                riskScore,
                riskLevel,
                concentrationRiskPct,
                avgAbsChange.setScale(SCALE_PERCENT, ROUNDING),
                cashPercent,
                recommendation
        );
    }


    private RiskResponse zeroRisk() {
        return new RiskResponse(
                0,
                "LOW",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(100).setScale(SCALE_PERCENT, ROUNDING),
                resolveRecommendation("LOW")
        );
    }

    private String resolveRiskLevel(int score) {
        if (score < 40)  return "LOW";
        if (score <= 70) return "MODERATE";
        if (score <= 85) return "HIGH";
        return "EXTREME";
    }

    private String resolveRecommendation(String riskLevel) {
        return switch (riskLevel) {
            case "LOW"      -> "Portfolio is well-balanced.";
            case "MODERATE" -> "Consider diversifying holdings.";
            case "HIGH"     -> "High concentration risk. Reduce largest position.";
            case "EXTREME"  -> "Portfolio is extremely concentrated. Immediate rebalancing recommended.";
            default         -> "";
        };
    }
}
