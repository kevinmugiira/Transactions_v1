package com.kevin.transactionPrac.Transactions.repositories;

import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotency_key);

    @Query("""
                select t from Transaction t 
                where t.sourceAccountId = :accountId or t.targetAccountId = :accountId
                order by t.created_at desc, t.id desc
           """)
    Page<Transaction> findHistory(@Param("accountId") Long accountId, Pageable pageable);

    Page<Transaction> findBySourceAccountIdOrTargetAccountId(Long src, Long tgt, Pageable p);

    Page<Transaction> findByTypeAndSourceAccountId(TransactionType type, Long accountId, Pageable p);

}
