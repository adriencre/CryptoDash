package com.cryptodash.repository;

import com.cryptodash.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Transaction> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
