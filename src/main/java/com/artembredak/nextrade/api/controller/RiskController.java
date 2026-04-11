package com.artembredak.nextrade.api.controller;

import com.artembredak.nextrade.api.dto.common.ApiResponse;
import com.artembredak.nextrade.api.dto.response.RiskResponse;
import com.artembredak.nextrade.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Risk", description = "Portfolio risk analytics")
public class RiskController {

    private final RiskService riskService;


    @GetMapping("/risk")
    @Operation(
            summary = "Calculate portfolio risk score",
            description = "Returns a composite risk score (0–100) with level and per-factor breakdown " +
                    "for the authenticated user's primary portfolio. " +
                    "Levels: LOW (<40) | MODERATE (40–70) | HIGH (71–85) | EXTREME (>85)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Risk analysis returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User or portfolio not found")
    })
    public ResponseEntity<ApiResponse<RiskResponse>> getRisk(Authentication authentication) {
        RiskResponse risk = riskService.calculateRisk(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(risk));
    }
}
