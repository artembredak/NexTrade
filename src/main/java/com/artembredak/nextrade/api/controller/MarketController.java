package com.artembredak.nextrade.api.controller;

import com.artembredak.nextrade.api.dto.common.ApiResponse;
import com.artembredak.nextrade.api.dto.response.CoinResponse;
import com.artembredak.nextrade.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Market", description = "Live cryptocurrency prices from CoinGecko")
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/prices")
    @Operation(
            summary = "List all coin prices",
            description = "Returns all tracked cryptocurrencies ordered by market cap descending. " +
                    "Prices are refreshed every 30 seconds by the background scheduler."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Coin list returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token")
    })
    public ResponseEntity<ApiResponse<List<CoinResponse>>> getAllPrices() {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getAllCoins()));
    }


    @GetMapping("/prices/{coinId}")
    @Operation(
            summary = "Get price for a single coin",
            description = "Returns current market data for the specified CoinGecko coin id " +
                    "(e.g. 'bitcoin', 'ethereum')."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Coin data returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Coin not found")
    })
    public ResponseEntity<ApiResponse<CoinResponse>> getCoinPrice(
            @Parameter(description = "CoinGecko coin id", example = "bitcoin")
            @PathVariable String coinId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getCoinById(coinId)));
    }
}
