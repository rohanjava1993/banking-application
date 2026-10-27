package com.example.banking.service;

import com.example.banking.dto.*;
import com.example.banking.entity.*;
import com.example.banking.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    public BankingService(
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        BigDecimal initialBalance = request.initialBalance();

        if (initialBalance == null ||
                initialBalance.signum() < 0 ||
                initialBalance.scale() > 2) {
            throw new IllegalArgumentException(
                "Initial balance must be non-negative "
                + "and have at most two decimal places"
            );
        }

        Account account = new Account(initialBalance);
        account = accountRepository.save(account);

        if (initialBalance.signum() > 0) {
            ledgerRepository.save(new LedgerEntry(
                account.getId(),
                TransactionType.DEPOSIT,
                initialBalance,
                account.getBalance(),
                null
            ));
        }

        return account;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() ->
                new NoSuchElementException("Account not found")
            )
            .getBalance();
    }

    @Transactional
    public Account deposit(
            UUID accountId,
            TransactionRequest request) {

        Account account = lockAccount(accountId);
        BigDecimal amount = validateAmount(request.amount());

        account.deposit(amount);

        ledgerRepository.save(new LedgerEntry(
            accountId,
            TransactionType.DEPOSIT,
            amount,
            account.getBalance(),
            null
        ));

        return account;
    }

    @Transactional
    public Account withdraw(
            UUID accountId,
            TransactionRequest request) {

        Account account = lockAccount(accountId);
        BigDecimal amount = validateAmount(request.amount());

        account.withdraw(amount);

        ledgerRepository.save(new LedgerEntry(
            accountId,
            TransactionType.WITHDRAWAL,
            amount,
            account.getBalance(),
            null
        ));

        return account;
    }

    @Transactional
    public Map<String, Object> transfer(TransferRequest request) {
        UUID fromId = request.fromAccountId();
        UUID toId = request.toAccountId();
        BigDecimal amount = validateAmount(request.amount());

        if (fromId.equals(toId)) {
            throw new IllegalArgumentException(
                "Source and destination accounts must differ"
            );
        }

        // Always acquire locks in a consistent order
        // to reduce the risk of deadlocks.
        UUID firstId = fromId.compareTo(toId) < 0
            ? fromId : toId;
        UUID secondId = fromId.compareTo(toId) < 0
            ? toId : fromId;

        Account first = lockAccount(firstId);
        Account second = lockAccount(secondId);

        Account source = fromId.equals(firstId) ? first : second;
        Account destination = toId.equals(firstId) ? first : second;

        // Both balance changes and ledger entries commit together.
        source.withdraw(amount);
        destination.deposit(amount);

        ledgerRepository.saveAll(List.of(
            new LedgerEntry(
                fromId,
                TransactionType.TRANSFER_OUT,
                amount,
                source.getBalance(),
                toId
            ),
            new LedgerEntry(
                toId,
                TransactionType.TRANSFER_IN,
                amount,
                destination.getBalance(),
                fromId
            )
        ));

        return Map.of(
            "fromAccountId", fromId,
            "toAccountId", toId,
            "amount", amount,
            "sourceBalance", source.getBalance(),
            "destinationBalance", destination.getBalance()
        );
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getTransactionHistory(
            UUID accountId) {

        if (!accountRepository.existsById(accountId)) {
            throw new NoSuchElementException("Account not found");
        }

        return ledgerRepository
            .findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    private Account lockAccount(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
            .orElseThrow(() ->
                new NoSuchElementException("Account not found")
            );
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null ||
                amount.signum() <= 0 ||
                amount.scale() > 2 ||
                amount.precision() > 19) {
            throw new IllegalArgumentException(
                "Amount must be positive, have at most "
                + "two decimal places, and fit the supported range"
            );
        }

        return amount;
    }
}