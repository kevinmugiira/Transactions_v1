package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionType;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;


    @Transactional
    public Transaction debit(Long accountId, BigDecimal amount, String description, String idempotencyKey) {

        Optional<Transaction> replay = findReplay(idempotencyKey);
        if (replay.isPresent()) return replay.get();

        Account account = lockOne(accountId);
        account.credit(amount);

        return transactions.save(new Transaction(
                TransactionType.DEPOSIT,
                amount,
                null,
                accountId,
                idempotencyKey,
                description
        ));
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, String description, String idempotencyKey) {
        Optional<Transaction> replay = findReplay(idempotencyKey);
        if (replay.isPresent()) return replay.get();

        Account account = lockOne(accountId);
        account.debit(amount);

        return transactions.save(new Transaction(
                TransactionType.WITHDRWAL,
                amount,
                accountId,
                null,
                idempotencyKey,
                description
        ));
    }


    private Optional<Transaction> findReplay(String key) {
        return key == null ? Optional.empty() : transactions.findByIndempotencyKey(key);
    }

    private Account lockOne(Long id) {
        return accounts
                .findAllByIdForUpdate(List.of(id))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
