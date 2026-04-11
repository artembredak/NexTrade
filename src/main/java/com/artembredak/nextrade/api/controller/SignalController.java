package com.artembredak.nextrade.api.controller;

import com.artembredak.nextrade.api.dto.common.ApiResponse;
import com.artembredak.nextrade.api.dto.request.CreateSignalRequest;
import com.artembredak.nextrade.api.dto.response.SignalResponse;
import com.artembredak.nextrade.service.SignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Signals", description = "Trading signals — active signals, history, admin create/close")
public class SignalController {

    private final SignalService signalService;

    @GetMapping("/api/signals")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active trading signals")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Active signal list returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token")
    })
    public ResponseEntity<ApiResponse<List<SignalResponse>>> getActiveSignals() {
        List<SignalResponse> signals = signalService.getActiveSignals();
        return ResponseEntity.ok(ApiResponse.ok(signals));
    }


    @GetMapping("/api/signals/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get closed signal history")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Closed signal list returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token")
    })
    public ResponseEntity<ApiResponse<List<SignalResponse>>> getSignalHistory() {
        List<SignalResponse> signals = signalService.getSignalHistory();
        return ResponseEntity.ok(ApiResponse.ok(signals));
    }


    @PostMapping("/api/admin/signals")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new trading signal (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Signal created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Caller does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Coin not found")
    })
    public ResponseEntity<ApiResponse<SignalResponse>> createSignal(
            @Valid @RequestBody CreateSignalRequest request,
            Authentication authentication) {

        SignalResponse signal = signalService.createSignal(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Signal created successfully", signal));
    }

    @PatchMapping("/api/admin/signals/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close an active trading signal (admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Signal closed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Signal is already closed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Caller does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Signal not found")
    })
    public ResponseEntity<ApiResponse<SignalResponse>> closeSignal(@PathVariable UUID id) {
        SignalResponse signal = signalService.closeSignal(id);
        return ResponseEntity.ok(ApiResponse.ok("Signal closed successfully", signal));
    }
}
