package com.artembredak.nextrade.api.controller;

import com.artembredak.nextrade.api.dto.common.ApiResponse;
import com.artembredak.nextrade.api.dto.common.PageResponse;
import com.artembredak.nextrade.api.dto.request.BuyRequest;
import com.artembredak.nextrade.api.dto.request.DepositRequest;
import com.artembredak.nextrade.api.dto.request.SellRequest;
import com.artembredak.nextrade.api.dto.request.WithdrawRequest;
import com.artembredak.nextrade.api.dto.response.TransactionResponse;
import com.artembredak.nextrade.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transactions", description = "Deposit, withdraw, buy, sell, and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @Operation(
            summary = "Deposit funds",
            description = "Adds the specified amount to the authenticated user's cash balance."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Deposit recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Portfolio not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        TransactionResponse response = transactionService.deposit(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Deposit successful", response));
    }

    @PostMapping("/withdraw")
    @Operation(
            summary = "Withdraw funds",
            description = "Deducts the specified amount from the authenticated user's cash balance. " +
                    "Fails with 400 if the balance is insufficient."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Withdrawal recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Insufficient balance or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Portfolio not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication
    ) {
        TransactionResponse response = transactionService.withdraw(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Withdrawal successful", response));
    }


    @PostMapping("/buy")
    @Operation(
            summary = "Buy a cryptocurrency",
            description = "Purchases the specified quantity of a coin at its current market price. " +
                    "Deducts total cost from cash balance and updates the portfolio asset position."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Buy order executed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Insufficient balance, price unavailable, or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Coin or portfolio not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> buy(
            @Valid @RequestBody BuyRequest request,
            Authentication authentication
    ) {
        TransactionResponse response = transactionService.buy(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Buy order executed", response));
    }
  @PostMapping("/sell")
    @Operation(
            summary = "Sell a cryptocurrency",
            description = "Sells the specified quantity of a coin at its current market price. " +
                    "Credits proceeds to cash balance and decreases the portfolio asset position. " +
                    "Deletes the asset record when quantity reaches zero."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Sell order executed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Insufficient quantity, price unavailable, or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Coin, asset, or portfolio not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> sell(
            @Valid @RequestBody SellRequest request,
            Authentication authentication
    ) {
        TransactionResponse response = transactionService.sell(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sell order executed", response));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get transaction history",
            description = "Returns a paginated list of all transactions for the authenticated user's " +
                    "portfolio, ordered by creation time descending."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Transaction history returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Missing or invalid Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Portfolio not found")
    })
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            Authentication authentication
    ) {
        PageResponse<TransactionResponse> history =
                transactionService.getHistory(authentication.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
