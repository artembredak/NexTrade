package com.artembredak.nextrade.service;

import com.artembredak.nextrade.api.dto.request.LoginRequest;
import com.artembredak.nextrade.api.dto.request.RegisterRequest;
import com.artembredak.nextrade.api.dto.response.AuthResponse;
import com.artembredak.nextrade.api.dto.response.UserResponse;
import com.artembredak.nextrade.api.exception.BusinessException;
import com.artembredak.nextrade.api.exception.UnauthorizedException;
import com.artembredak.nextrade.api.security.JwtTokenProvider;
import com.artembredak.nextrade.domain.model.Portfolio;
import com.artembredak.nextrade.domain.model.Role;
import com.artembredak.nextrade.domain.model.User;
import com.artembredak.nextrade.domain.repository.PortfolioRepository;
import com.artembredak.nextrade.domain.repository.UserRepository;
import com.artembredak.nextrade.infrastructure.cache.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already in use", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already in use", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .isActive(true)
                .isDeleted(false)
                .build();

        userRepository.save(user);
        log.info("Registered new user: userId={}, email={}", user.getId(), user.getEmail());

        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .name("My Portfolio")
                .cashBalance(BigDecimal.ZERO)
                .build();

        portfolioRepository.save(portfolio);
        log.info("Created default portfolio for userId={}", user.getId());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated", HttpStatus.FORBIDDEN);
        }

        log.info("User logged in: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    public void logout(String refreshToken) {
        refreshTokenStore.delete(refreshToken);
        log.info("Refresh token invalidated");
    }

    public AuthResponse refresh(String refreshToken) {
        String email = refreshTokenStore.getEmail(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            refreshTokenStore.delete(refreshToken);
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        refreshTokenStore.delete(refreshToken);
        log.info("Refresh token rotated for userId={}", user.getId());

        return buildAuthResponse(user);
    }

    // --- private helpers ---

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        refreshTokenStore.store(newRefreshToken, user.getEmail(), jwtTokenProvider.getRefreshExpiration());

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayUsername(),
                user.getRole().name()
        );

        return new AuthResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.getAccessExpiration(),
                userResponse
        );
    }
}
