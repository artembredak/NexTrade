package com.artembredak.nextrade.api.controller;

import com.artembredak.nextrade.api.dto.common.ApiResponse;
import com.artembredak.nextrade.api.dto.response.AssetResponse;
import com.artembredak.nextrade.api.dto.response.PortfolioChartResponse;
import com.artembredak.nextrade.api.dto.response.PortfolioSummaryResponse;
import com.artembredak.nextrade.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio summary, assets, P&L, and historical chart data")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/summary")
    @Operation(
            summary = "Get portfolio summary",
            description = "Returns total value, cash/crypto split, 24 h change, and asset count " +
                    "for the authenticated user's portfolio."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Portfolio summary returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Portfolio not found for authenticated user")
    })
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getSummary() {
        PortfolioSummaryResponse summary = portfolioService.getSummary(getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/assets")
    @Operation(
            summary = "List portfolio assets with P&L",
            description = "Returns all assets with positive quantity, sorted by current value " +
                    "descending, including per-asset P&L in USD and percentage."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Asset list returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Portfolio not found for authenticated user")
    })
    public ResponseEntity<ApiResponse<List<AssetResponse>>> getAssets() {
        List<AssetResponse> assets = portfolioService.getAssets(getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.ok(assets));
    }

    @GetMapping("/chart")
    @Operation(
            summary = "Get portfolio value history",
            description = "Returns historical total-value snapshots for the given period. " +
                    "Accepted period values: 1d, 1w, 1m, 3m."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Chart data returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid period value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Portfolio not found for authenticated user")
    })
    public ResponseEntity<ApiResponse<PortfolioChartResponse>> getChart(
            @Parameter(description = "Time period: 1d | 1w | 1m | 3m", example = "1w")
            @RequestParam(defaultValue = "1w") String period
    ) {
        PortfolioChartResponse chart = portfolioService.getChart(getCurrentUserEmail(), period);
        return ResponseEntity.ok(ApiResponse.ok(chart));
    }


    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
