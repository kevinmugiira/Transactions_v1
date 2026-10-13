package com.kevin.transactionPrac.Transactions.repositories;

import com.kevin.transactionPrac.Transactions.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIndempotencyKey(String key);
    Page<Transaction> findBySourceAccountIdOrTargetAccountId(Long src, Long tgt, Pageable p);

}
