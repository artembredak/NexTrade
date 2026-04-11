package com.artembredak.nextrade.service;

import com.artembredak.nextrade.api.dto.common.PageResponse;
import com.artembredak.nextrade.api.dto.request.BuyRequest;
import com.artembredak.nextrade.api.dto.request.DepositRequest;
import com.artembredak.nextrade.api.dto.request.SellRequest;
import com.artembredak.nextrade.api.dto.request.WithdrawRequest;
import com.artembredak.nextrade.api.dto.response.TransactionResponse;
import com.artembredak.nextrade.api.exception.BusinessException;
import com.artembredak.nextrade.api.exception.NotFoundException;
import com.artembredak.nextrade.domain.model.*;
import com.artembredak.nextrade.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private static final int SCALE = 8;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final CoinRepository coinRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;


    public TransactionResponse deposit(String userEmail, DepositRequest request) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        BigDecimal newBalance = portfolio.getCashBalance()
                .add(request.amount())
                .setScale(SCALE, ROUNDING);
        portfolio.setCashBalance(newBalance);
        portfolioRepository.save(portfolio);

        Transaction tx = Transaction.builder()
                .portfolio(portfolio)
                .type(TransactionType.DEPOSIT)
                .amount(request.amount().setScale(SCALE, ROUNDING))
                .description(request.description())
                .build();
        transactionRepository.save(tx);

        log.info("Deposit completed: portfolioId={}, amount={}, newBalance={}",
                portfolio.getId(), request.amount(), newBalance);

        return toResponse(tx);
    }

    public TransactionResponse withdraw(String userEmail, WithdrawRequest request) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        if (portfolio.getCashBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(
                    "Insufficient balance: available=" + portfolio.getCashBalance()
                            + ", requested=" + request.amount(),
                    HttpStatus.BAD_REQUEST);
        }

        BigDecimal newBalance = portfolio.getCashBalance()
                .subtract(request.amount())
                .setScale(SCALE, ROUNDING);
        portfolio.setCashBalance(newBalance);
        portfolioRepository.save(portfolio);

        Transaction tx = Transaction.builder()
                .portfolio(portfolio)
                .type(TransactionType.WITHDRAWAL)
                .amount(request.amount().setScale(SCALE, ROUNDING))
                .description(request.description())
                .build();
        transactionRepository.save(tx);

        log.info("Withdrawal completed: portfolioId={}, amount={}, newBalance={}",
                portfolio.getId(), request.amount(), newBalance);

        return toResponse(tx);
    }


    public TransactionResponse buy(String userEmail, BuyRequest request) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        Coin coin = coinRepository.findById(request.coinId())
                .orElseThrow(() -> new NotFoundException("Coin not found: " + request.coinId()));

        if (coin.getCurrentPrice() == null) {
            throw new BusinessException("Price not available for coin: " + request.coinId(),
                    HttpStatus.BAD_REQUEST);
        }

        BigDecimal currentPrice = coin.getCurrentPrice();
        BigDecimal totalCost = request.quantity()
                .multiply(currentPrice)
                .setScale(SCALE, ROUNDING);

        if (portfolio.getCashBalance().compareTo(totalCost) < 0) {
            throw new BusinessException(
                    "Insufficient balance: available=" + portfolio.getCashBalance()
                            + ", required=" + totalCost,
                    HttpStatus.BAD_REQUEST);
        }

        portfolio.setCashBalance(portfolio.getCashBalance().subtract(totalCost).setScale(SCALE, ROUNDING));
        portfolioRepository.save(portfolio);

        portfolioAssetRepository.findByPortfolioIdAndCoinId(portfolio.getId(), coin.getId())
                .ifPresentOrElse(
                        existing -> {
                            BigDecimal oldQty = existing.getQuantity();
                            BigDecimal newQty = oldQty.add(request.quantity());
                            // (old_qty * old_avg + new_qty * buy_price) / (old_qty + new_qty)
                            BigDecimal newAvg = oldQty.multiply(existing.getAvgBuyPrice())
                                    .add(request.quantity().multiply(currentPrice))
                                    .divide(newQty, SCALE, ROUNDING);
                            existing.setQuantity(newQty.setScale(SCALE, ROUNDING));
                            existing.setAvgBuyPrice(newAvg);
                            portfolioAssetRepository.save(existing);
                            log.info("Asset updated after buy: portfolioId={}, coinId={}, newQty={}, newAvg={}",
                                    portfolio.getId(), coin.getId(), newQty, newAvg);
                        },
                        () -> {
                            PortfolioAsset asset = PortfolioAsset.builder()
                                    .portfolio(portfolio)
                                    .coin(coin)
                                    .quantity(request.quantity().setScale(SCALE, ROUNDING))
                                    .avgBuyPrice(currentPrice.setScale(SCALE, ROUNDING))
                                    .build();
                            portfolioAssetRepository.save(asset);
                            log.info("New asset created after buy: portfolioId={}, coinId={}, qty={}, avgBuyPrice={}",
                                    portfolio.getId(), coin.getId(), request.quantity(), currentPrice);
                        }
                );

        Transaction tx = Transaction.builder()
                .portfolio(portfolio)
                .coin(coin)
                .type(TransactionType.BUY)
                .amount(totalCost)
                .quantity(request.quantity().setScale(SCALE, ROUNDING))
                .pricePerCoin(currentPrice.setScale(SCALE, ROUNDING))
                .build();
        transactionRepository.save(tx);

        log.info("Buy completed: portfolioId={}, coinId={}, qty={}, totalCost={}",
                portfolio.getId(), coin.getId(), request.quantity(), totalCost);

        return toResponse(tx);
    }


    public TransactionResponse sell(String userEmail, SellRequest request) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        Coin coin = coinRepository.findById(request.coinId())
                .orElseThrow(() -> new NotFoundException("Coin not found: " + request.coinId()));

        PortfolioAsset asset = portfolioAssetRepository
                .findByPortfolioIdAndCoinId(portfolio.getId(), coin.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Asset not found: coinId=" + request.coinId()
                                + " in portfolioId=" + portfolio.getId()));

        if (asset.getQuantity().compareTo(request.quantity()) < 0) {
            throw new BusinessException(
                    "Insufficient quantity: available=" + asset.getQuantity()
                            + ", requested=" + request.quantity(),
                    HttpStatus.BAD_REQUEST);
        }

        if (coin.getCurrentPrice() == null) {
            throw new BusinessException("Price not available for coin: " + request.coinId(),
                    HttpStatus.BAD_REQUEST);
        }

        BigDecimal currentPrice = coin.getCurrentPrice();
        BigDecimal proceeds = request.quantity()
                .multiply(currentPrice)
                .setScale(SCALE, ROUNDING);

        portfolio.setCashBalance(portfolio.getCashBalance().add(proceeds).setScale(SCALE, ROUNDING));
        portfolioRepository.save(portfolio);

        BigDecimal remainingQty = asset.getQuantity()
                .subtract(request.quantity())
                .setScale(SCALE, ROUNDING);

        if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
            portfolioAssetRepository.delete(asset);
            log.info("Asset deleted after full sell: portfolioId={}, coinId={}",
                    portfolio.getId(), coin.getId());
        } else {
            asset.setQuantity(remainingQty);
            portfolioAssetRepository.save(asset);
            log.info("Asset reduced after sell: portfolioId={}, coinId={}, remaining={}",
                    portfolio.getId(), coin.getId(), remainingQty);
        }

        Transaction tx = Transaction.builder()
                .portfolio(portfolio)
                .coin(coin)
                .type(TransactionType.SELL)
                .amount(proceeds)
                .quantity(request.quantity().setScale(SCALE, ROUNDING))
                .pricePerCoin(currentPrice.setScale(SCALE, ROUNDING))
                .build();
        transactionRepository.save(tx);

        log.info("Sell completed: portfolioId={}, coinId={}, qty={}, proceeds={}",
                portfolio.getId(), coin.getId(), request.quantity(), proceeds);

        return toResponse(tx);
    }


    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getHistory(String userEmail, int page, int size) {
        Portfolio portfolio = getPortfolioByUserEmail(userEmail);

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> responsePage = transactionRepository
                .findByPortfolioIdWithCoinOrderByCreatedAtDesc(portfolio.getId(), pageable)
                .map(this::toResponse);

        log.debug("Transaction history loaded: portfolioId={}, page={}, size={}, total={}",
                portfolio.getId(), page, size, responsePage.getTotalElements());

        return PageResponse.from(responsePage);
    }

    private Portfolio getPortfolioByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        return portfolioRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Portfolio not found for user: " + email));
    }

    private TransactionResponse toResponse(Transaction tx) {
        String coinId = tx.getCoin() != null ? tx.getCoin().getId() : null;
        String coinSymbol = tx.getCoin() != null ? tx.getCoin().getSymbol() : null;

        return new TransactionResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getQuantity(),
                tx.getPricePerCoin(),
                coinId,
                coinSymbol,
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
