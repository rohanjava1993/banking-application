package com.example.banking.controller;

import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.entity.Account;
import com.example.banking.service.BankingService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public Map<String, Object> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        Account account = bankingService.createAccount(request);

        return Map.of(
            "accountId", account.getId(),
            "balance", account.getBalance()
        );
    }

    @GetMapping("/{accountId}/balance")
    public Map<String, Object> getBalance(
            @PathVariable UUID accountId) {

        BigDecimal balance = bankingService.getBalance(accountId);

        return Map.of(
            "accountId", accountId,
            "balance", balance
        );
    }
}