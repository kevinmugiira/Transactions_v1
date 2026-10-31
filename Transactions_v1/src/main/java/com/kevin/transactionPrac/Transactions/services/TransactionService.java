package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.config.KafkaConfig;
import com.kevin.transactionPrac.Transactions.exceptions.AccountNotFoundException;
import com.kevin.transactionPrac.Transactions.messaging.OutboxService;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionType;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import com.kevin.transactionPrac.Transactions.security.Caller;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

//import static sun.swing.SwingUtilities2.submit;


/**
 * The "front door": checks who is asking, accepts the request quickly and records it as PENDING.
 * It does NOT touch any balance. That is done later, asynchronously, by {@link TransactionProcessor}.
 *
 * In ONE database transaction this inserts the PENDING ledger row and an outbox row holding the
 * command "process transaction N". Both commit together or not at all.
 *
 * Authorization rules:
 *  - money can only LEAVE (withdraw, transfer) or ENTER via deposit an account its owner controls;
 *    not even admins move customers' money
 *  - a transfer may go INTO any existing account
 *  - reads are allowed for the people involved, and for admins
 */

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final OutboxService outbox;


    public Submission submitDeposit(
            Caller caller,
            Long accountId,
            BigDecimal amount,
            String description,
            String idempotencyKey
    ) {
        return submit(caller, TransactionType.DEPOSIT, amount, null, accountId, description, idempotencyKey);
    }

    private Submission submit(
            Caller caller,
            TransactionType type,
            BigDecimal amount,
            Long sourceId,
            Long targetId,
            String description,
            String idempotencyKey
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (idempotencyKey != null) {
            Optional<Transaction> replay =
                    transactions.findByInitiatedByAndIdempotencyKey(caller.userId(), idempotencyKey);
            if (replay.isPresent()) {
                return new Submission(replay.get(), true);
            }
        }

        // Ownership: the account the caller acts on must be theirs. A transfer's target only has to exist.
        if (type == TransactionType.DEPOSIT) {
            requireOwned(targetId, caller);
        } else {
            requireOwned(sourceId, caller);
            if (targetId != null && accounts.existsById(targetId)) {
                throw new AccountNotFoundException(targetId);
            }
        }

        Transaction tx = transactions.save(new Transaction(
                type,
                amount,
                sourceId,
                targetId,
                caller.userId(),
                idempotencyKey,
                description
        ));

        outbox.stage(
                UUID.randomUUID().toString(),
                KafkaConfig.COMMANDS,
                String.valueOf(tx.routingAccountId()),
                "TransactionRequested",
                String.valueOf(tx.getId())
        );
        return new Submission(tx, false);
    }

    /** Not found and not yours look identical on purpose (see AccountService.get). */
    private void requireOwned(Long accountId, Caller caller) {
        Account account = accounts
                .findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId))
        ;

        if (!account.isOwnedBy(caller.userId())) {
            throw new AccountNotFoundException(accountId);
        }
    }



//    @Transactional
//    public Transaction deposit(
//                                Long accountId,
//                                BigDecimal amount,
//                                String description,
//                                String idempotencyKey) {
//
//        Optional<Transaction> replay = findReplay(idempotencyKey);
//        if (replay.isPresent()) return replay.get();
//
//        Account account = lockOne(accountId);
//        account.credit(amount);
//
//        return transactions.save(new Transaction(
//                TransactionType.DEPOSIT,
//                amount,
//                null,
//                accountId,
//                idempotencyKey,
//                description
//        ));
//    }
//
//    @Transactional
//    public Transaction withdraw(
//                                Long accountId,
//                                BigDecimal amount,
//                                String description,
//                                String idempotencyKey) {
//
//        Optional<Transaction> replay = findReplay(idempotencyKey);
//        if (replay.isPresent()) return replay.get();
//
//        Account account = lockOne(accountId);
//        account.debit(amount);
//
//        return transactions.save(new Transaction(
//                TransactionType.WITHDRWAL,
//                amount,
//                accountId,
//                null,
//                idempotencyKey,
//                description
//        ));
//    }
//
//    @Transactional
//    public Transaction transfer(
//                                Long sourceId,
//                                Long targetId,
//                                BigDecimal amount,
//                                String description,
//                                String idempotencyKey) {
//
//        if (sourceId.equals(targetId)) {
//            throw new IllegalArgumentException("Cannot transfer to same account");
//        }
//
//        Optional<Transaction> replay = findReplay(idempotencyKey);
//        if (replay.isPresent()) return replay.get();
//
//        Map<Long, Account> locked = accounts.findAllByIdForUpdate(List.of(sourceId, targetId))
//                                            .stream()
//                                            .collect(
//                                                    Collectors
//                                                            .toMap(Account::getId, Function.identity())
//                                            );
//        Account source = Optional
//                .ofNullable(locked.get(sourceId))
//                .orElseThrow(() -> new AccountNotFoundException(sourceId));
//
//        Account target = Optional
//                .ofNullable(locked.get(targetId))
//                .orElseThrow(() -> new AccountNotFoundException(targetId));
//
//        if (!source.getCurrency().equals(target.getCurrency())) {
//            throw new IllegalArgumentException("Currency mismatch between accounts");
//        }
//
//        source.debit(amount);
//        target.credit(amount);
//
//        return transactions.save(new Transaction(
//                TransactionType.TRANSFER,
//                amount,
//                sourceId,
//                targetId,
//                idempotencyKey,
//                description
//        ));
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<Transaction> findByIdempotencyKey(String key) {
//        return transactions.findByIdempotencyKey(key);
//    }
//
//    @Transactional(readOnly = true)
//    public Transaction get(Long id) {
//        return transactions
//                .findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Transaction " + id + " not found!"));
//    }
//
//    @Transactional(readOnly = true)
//    public Page<Transaction> history(Long accountId, Pageable pageable) {
//        return transactions.findHistory(accountId, pageable);
//    }
//
//
//    private Optional<Transaction> findReplay(String key) {
//        return key == null ? Optional.empty() : transactions.findByIdempotencyKey(key);
//    }
//
//    private Account lockOne(Long id) {
//        return accounts
//                .findAllByIdForUpdate(List.of(id))
//                .stream()
//                .findFirst()
//                .orElseThrow(() -> new AccountNotFoundException(id));
//    }
}
