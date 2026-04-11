package com.artembredak.nextrade.service;

import com.artembredak.nextrade.api.dto.request.CreateSignalRequest;
import com.artembredak.nextrade.api.dto.response.SignalResponse;
import com.artembredak.nextrade.api.exception.BusinessException;
import com.artembredak.nextrade.api.exception.NotFoundException;
import com.artembredak.nextrade.domain.model.Coin;
import com.artembredak.nextrade.domain.model.SignalStatus;
import com.artembredak.nextrade.domain.model.TradingSignal;
import com.artembredak.nextrade.domain.model.User;
import com.artembredak.nextrade.domain.repository.CoinRepository;
import com.artembredak.nextrade.domain.repository.TradingSignalRepository;
import com.artembredak.nextrade.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SignalService {

    private final TradingSignalRepository tradingSignalRepository;
    private final UserRepository userRepository;
    private final CoinRepository coinRepository;

    public List<SignalResponse> getActiveSignals() {
        List<TradingSignal> signals = tradingSignalRepository
                .findByStatusWithCoinOrderByCreatedAtDesc(SignalStatus.ACTIVE);
        log.debug("Fetched {} active signals", signals.size());
        return signals.stream().map(this::toResponse).toList();
    }

    public List<SignalResponse> getSignalHistory() {
        List<TradingSignal> signals = tradingSignalRepository
                .findByStatusWithCoinOrderByCreatedAtDesc(SignalStatus.CLOSED);
        log.debug("Fetched {} closed signals", signals.size());
        return signals.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SignalResponse createSignal(CreateSignalRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new NotFoundException("User not found: " + adminEmail));

        Coin coin = coinRepository.findById(request.coinId())
                .orElseThrow(() -> new NotFoundException("Coin", request.coinId()));

        TradingSignal signal = TradingSignal.builder()
                .coin(coin)
                .createdBy(admin)
                .signalType(request.signalType())
                .confidencePercent(request.confidencePercent())
                .entryPrice(request.entryPrice())
                .targetPrice(request.targetPrice())
                .stopLossPrice(request.stopLossPrice())
                .rationale(request.rationale())
                .status(SignalStatus.ACTIVE)
                .build();

        TradingSignal saved = tradingSignalRepository.save(signal);
        log.info("Trading signal created: signalId={}, coinId={}, type={}, createdBy={}",
                saved.getId(), coin.getId(), request.signalType(), adminEmail);

        return toResponse(saved);
    }


    @Transactional
    public SignalResponse closeSignal(UUID signalId) {
        TradingSignal signal = tradingSignalRepository.findById(signalId)
                .orElseThrow(() -> new NotFoundException("TradingSignal", signalId));

        if (signal.getStatus() == SignalStatus.CLOSED) {
            throw new BusinessException("Signal already closed", HttpStatus.BAD_REQUEST);
        }

        signal.setStatus(SignalStatus.CLOSED);
        signal.setClosedAt(OffsetDateTime.now());

        TradingSignal saved = tradingSignalRepository.save(signal);
        log.info("Trading signal closed: signalId={}", signalId);

        return toResponse(saved);
    }

    private SignalResponse toResponse(TradingSignal signal) {
        Coin coin = signal.getCoin();
        return new SignalResponse(
                signal.getId(),
                coin.getId(),
                coin.getName(),
                coin.getSymbol(),
                signal.getSignalType(),
                signal.getConfidencePercent(),
                signal.getEntryPrice(),
                signal.getTargetPrice(),
                signal.getStopLossPrice(),
                signal.getRationale(),
                signal.getStatus(),
                signal.getClosedAt(),
                signal.getCreatedAt()
        );
    }
}
