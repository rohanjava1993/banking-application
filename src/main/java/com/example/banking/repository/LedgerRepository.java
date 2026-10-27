package com.example.banking.repository;

import com.example.banking.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerRepository
        extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry>
        findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}