package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.exceptions.AccountNotFoundException;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionType;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;


    @Transactional
    public Transaction deposit(
                                Long accountId,
                                BigDecimal amount,
                                String description,
                                String idempotencyKey) {

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
    public Transaction withdraw(
                                Long accountId,
                                BigDecimal amount,
                                String description,
                                String idempotencyKey) {

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

    @Transactional
    public Transaction transfer(
                                Long sourceId,
                                Long targetId,
                                BigDecimal amount,
                                String description,
                                String idempotencyKey) {

        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        Optional<Transaction> replay = findReplay(idempotencyKey);
        if (replay.isPresent()) return replay.get();

        Map<Long, Account> locked = accounts.findAllByIdForUpdate(List.of(sourceId, targetId))
                                            .stream()
                                            .collect(
                                                    Collectors
                                                            .toMap(Account::getId, Function.identity())
                                            );
        Account source = Optional
                .ofNullable(locked.get(sourceId))
                .orElseThrow(() -> new AccountNotFoundException(sourceId));

        Account target = Optional
                .ofNullable(locked.get(targetId))
                .orElseThrow(() -> new AccountNotFoundException(targetId));

        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }

        source.debit(amount);
        target.credit(amount);

        return transactions.save(new Transaction(
                TransactionType.TRANSFER,
                amount,
                sourceId,
                targetId,
                idempotencyKey,
                description
        ));
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> findByIdempotencyKey(String key) {
        return transactions.findByIndempotencyKey(key);
    }

    @Transactional(readOnly = true)
    public Transaction get(Long id) {
        return transactions
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction " + id + " not found!"));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> history(Long accountId, Pageable pageable) {
        return transactions.findHistory(accountId, pageable);
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
