package com.example.banking.controller;

import com.example.banking.dto.TransactionRequest;
import com.example.banking.dto.TransferRequest;
import com.example.banking.entity.Account;
import com.example.banking.entity.LedgerEntry;
import com.example.banking.service.BankingService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final BankingService bankingService;

    public TransactionController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/{accountId}/deposit")
    public Map<String, Object> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {

        Account account =
            bankingService.deposit(accountId, request);

        return Map.of(
            "message", "Deposit successful",
            "accountId", account.getId(),
            "balance", account.getBalance()
        );
    }

    @PostMapping("/{accountId}/withdraw")
    public Map<String, Object> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {

        Account account =
            bankingService.withdraw(accountId, request);

        return Map.of(
            "message", "Withdrawal successful",
            "accountId", account.getId(),
            "balance", account.getBalance()
        );
    }

    @PostMapping("/transfer")
    public Map<String, Object> transfer(
            @Valid @RequestBody TransferRequest request) {

        return bankingService.transfer(request);
    }

    @GetMapping("/accounts/{accountId}/history")
    public List<LedgerEntry> getHistory(
            @PathVariable UUID accountId) {

        return bankingService.getTransactionHistory(accountId);
    }
}