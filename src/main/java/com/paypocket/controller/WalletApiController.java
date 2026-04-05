package com.paypocket.controller;


import com.paypocket.dto.*;
import com.paypocket.exception.PayPocketException;
import com.paypocket.model.Currency;
import com.paypocket.model.User;
import com.paypocket.model.Wallet;
import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API для кошельков.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletApiController {

    private final WalletService walletService;
    private final UserService userService;

    public  WalletApiController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    /**
     * POST /api/v1/wallets?userId=... — создать кошелёк.
     */
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateWalletRequest request) {
        Currency currency = Currency.valueOf(request.getCurrency().toUpperCase());
        Wallet wallet = walletService.createWallet(userId, request.getName(), currency);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(WalletResponse.from(wallet));
    }

    /**
     * GET /api/v1/wallets/{id} — информация о кошельке.
     */
    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID id) {
        Wallet wallet = walletService.getWallet(id);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    /**
     * GET /api/v1/wallets?userId=... — все кошельки пользователя.
     */
    @GetMapping
    public ResponseEntity<List<WalletResponse>> getUserWallets(@RequestParam UUID userId) {
        List<WalletResponse> wallets = walletService.getUserWallets(userId).stream()
                .map(WalletResponse::from)
                .toList();
        return ResponseEntity.ok(wallets);
    }

    /**
     * POST /api/v1/wallets/{id}/deposit — пополнение.
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody DepositRequest request) {
        Wallet wallet = walletService.deposit(id, request.getAmount());
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    /**
     * POST /api/v1/wallets/{id}/transfer — перевод.
     */
    @PostMapping("/{id}/transfer")
    public ResponseEntity<TransferResult> transfer(
            @PathVariable UUID id,
            @Valid @RequestBody TransferRequest request) {
        User recipient = userService.getByUsername(request.getRecipientUsername());
        Wallet senderWallet = walletService.getWallet(id);

        Wallet receiverWallet = walletService.getUserWallets(recipient.getId()).stream()
                .filter(w -> w.getCurrency() == senderWallet.getCurrency())
                .findFirst()
                .orElseThrow(() -> new PayPocketException(
                        "У получателя нет кошельков в валюте " + senderWallet.getCurrency()
                ));

        TransferResult result = walletService.transfer(id, receiverWallet.getId(), request.getAmount());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/wallets/{id}/transactions — история операций.
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        List<TransactionResponse> transactions = walletService
                .getTransactionHistory(id, pageNumber, pageSize)
                .map(TransactionResponse::from)
                .getContent();
        return ResponseEntity.ok(transactions);
    }

}
