package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;


    @Transactional
    public Transaction debit(Long accountId, BigDecimal amount, String description, String idempotencyKey) {

        Optional<Transaction> replay = findReplay(idempotencyKey);
    }
}
