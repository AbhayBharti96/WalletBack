package com.loyaltyService.wallet_service.controller;

import com.loyaltyService.wallet_service.dto.*;
import com.loyaltyService.wallet_service.entity.LedgerEntry;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.repository.LedgerEntryRepository;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CQRS Controller — GET endpoints use WalletQueryService (Redis cached),
 * write endpoints use WalletCommandService (cache-evicting).
 */
@Slf4j
@RestController
@RequestMapping("api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet balance, top-up, transfer, ledger")
public class WalletController {

    // CQRS: Query side (cached reads)
    private final WalletQueryService walletQueryService;
    // CQRS: Command side (writes + cache eviction)
    private final WalletCommandService walletCommandService;
    private final LedgerEntryRepository ledgerRepo;

    // ── Balance ───────────────────────────────────────────────────────────────
    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> balance(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Balance fetched", walletQueryService.getBalance(userId)));
    }

    // // ── Topup ─────────────────────────────────────────────────────────────────
    // @PostMapping("/internal/topup")
    // @Operation(summary = "Top up wallet")
    // public ResponseEntity<ApiResponse<Void>> topup(
    // @RequestHeader("X-User-Id") Long userId,
    // @Valid @RequestBody TopupRequest req) {
    // walletService.topup(userId, req.getAmount(), req.getIdempotencyKey());
    // return ResponseEntity.ok(ApiResponse.ok("Top-up successful"));
    // }

    // ── Transfer ──────────────────────────────────────────────────────────────
    @PostMapping("/transfer")
    @Operation(summary = "Transfer to another user")
    public ResponseEntity<ApiResponse<Void>> transfer(
            @RequestHeader("X-User-Id") Long senderId,
            @Valid @RequestBody TransferRequest req) {
        walletCommandService.transfer(senderId, req.getReceiverId(), req.getAmount(),
                req.getIdempotencyKey(), req.getDescription());
        return ResponseEntity.ok(ApiResponse.ok("Transfer successful"));
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────
    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw from wallet")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody WithdrawRequest req) {
        walletCommandService.withdraw(userId, req.getAmount());
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal successful"));
    }

    // ── Transaction history (paginated) ──────────────────────────────────────
    @GetMapping("/transactions")
    @Operation(summary = "Paginated transaction history")
    public ResponseEntity<Page<Transaction>> transactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(walletQueryService.getTransactions(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    // ── Ledger (paginated) ────────────────────────────────────────────────────
    @GetMapping("/ledger")
    @Operation(summary = "Paginated ledger entries")
    public ResponseEntity<Page<LedgerEntry>> ledger(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ledgerRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)));
    }

    // ── Statement (JSON) ──────────────────────────────────────────────────────
    @GetMapping("/statement")
    @Operation(summary = "Account statement for a date range (JSON)")
    public ResponseEntity<List<Transaction>> statement(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt = LocalDate.parse(to).atTime(23, 59, 59);
        return ResponseEntity.ok(walletQueryService.getStatement(userId, fromDt, toDt));
    }

    // ── Statement CSV download ────────────────────────────────────────────────
    @GetMapping("/statement/download")
    @Operation(summary = "Download statement as CSV")
    public void downloadStatement(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String from,
            @RequestParam String to,
            HttpServletResponse response) throws Exception {
        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt = LocalDate.parse(to).atTime(23, 59, 59);
        List<Transaction> txns = walletQueryService.getStatement(userId, fromDt, toDt);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=statement_" + userId + ".csv");
        PrintWriter writer = response.getWriter();
        writer.println("ID,Type,Amount,Status,Reference,Date");
        for (Transaction t : txns)
            writer.printf("%d,%s,%s,%s,%s,%s%n", t.getId(), t.getType(), t.getAmount(),
                    t.getStatus(), t.getReferenceId(), t.getCreatedAt());
        writer.flush();
    }

    // ── Internal — create wallet for new user ─────────────────────────────────
    @PostMapping("/internal/create")
    @Operation(summary = "Internal — create wallet for new user")
    public ResponseEntity<ApiResponse<Void>> createWallet(
            @RequestParam Long userId) {
        walletCommandService.createWallet(userId);
        return ResponseEntity.ok(ApiResponse.ok("Wallet created successfully"));
    }

    // ── Internal — credit (cashback or points redemption) ────────────────────
    // FIX: Added optional `source` param ("REDEEM" | "CASHBACK").
    // Defaults to "CASHBACK" for backward compatibility.
    // This lets the transaction history distinguish point redemptions from topup
    // cashback.
    @PostMapping("/internal/credit")
    @Operation(summary = "Internal cashback/redeem credit (service-to-service only)")
    public ResponseEntity<ApiResponse<Void>> internalCredit(
            @RequestParam Long userId,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam(defaultValue = "CASHBACK") String source) {
        walletCommandService.creditInternal(userId, amount, source);
        return ResponseEntity.ok(ApiResponse.ok("Credit applied"));
    }
}
