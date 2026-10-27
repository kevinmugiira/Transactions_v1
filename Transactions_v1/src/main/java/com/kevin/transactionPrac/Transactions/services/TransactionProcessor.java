package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.exceptions.AccountNotFoundException;
import com.kevin.transactionPrac.Transactions.exceptions.CurrencyMismatchException;
import com.kevin.transactionPrac.Transactions.messaging.OutboxService;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionStatus;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import org.kevin.events.Topics;
import org.kevin.events.TransactionCompletedEvent;
import org.kevin.events.TransactionFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);

    private final TransactionRepository transactions;
    private final AccountRepository accounts;
    private final OutboxService outbox;
    private final JsonMapper json;


    @Transactional
    public void process(Long transactionId) {
        Optional<Transaction> found = transactions.findByIdForUpdate(transactionId);

        if (found.isEmpty()) {
            log.warn("Ignoring command for unknown transaction {}", transactionId);
            return;
        }
        Transaction tx = found.get();

        if (tx.getStatus() != TransactionStatus.PENDING) {
            log.info("Transaction {} is already, {} skipping", tx.getId(), tx.getStatus());
            return;
        }
    }

    private void apply(Transaction tx) {
        switch (tx.getType()) {
            case DEPOSIT -> lockOne(tx.getTargetAccountId()).credit(tx.getAmount());
            case WITHDRWAL -> lockOne(tx.getSourceAccountId()).debit(tx.getAmount());
            case TRANSFER -> applyTransfer(tx);
        }
    }

    private void applyTransfer(Transaction tx) {
        Long sourceId = tx.getSourceAccountId();
        Long targetId = tx.getTargetAccountId();

        Map<Long, Account> locked = accounts
                .findAllByIdForUpdate(List.of(sourceId, targetId))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()))
        ;

        Account source = Optional
                .ofNullable(locked.get(sourceId))
                .orElseThrow(() -> new AccountNotFoundException(sourceId))
        ;

        Account target = Optional
                .ofNullable(locked.get(targetId))
                .orElseThrow(() -> new AccountNotFoundException(targetId))
        ;

        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new CurrencyMismatchException("Currency mismatch between accounts");
        }

        source.credit(tx.getAmount());
        target.debit(tx.getAmount());
    }

    private void stageCompleted(Transaction tx) {
        String eventId = UUID.randomUUID().toString();
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                eventId,
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getSourceAccountId(),
                tx.getTargetAccountId(),
                Instant.now()
        );
        outbox.stage(
                eventId,
                Topics.TRANSACTION_EVENTS,
                String.valueOf(tx.routingAccountId()),
                "TransactionCompleted",
                json.writeValueAsString(event)
        );
    }

    private void stageFailed(Transaction tx) {
        String eventId = UUID.randomUUID().toString();
        TransactionFailedEvent event = new TransactionFailedEvent(
                eventId,
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getSourceAccountId(),
                tx.getTargetAccountId(),
                tx.getFailureReason(),
                Instant.now()
        );
        outbox.stage(
                eventId,
                Topics.TRANSACTION_EVENTS,
                String.valueOf(tx.routingAccountId()),
                "TransactionFailed",
                json.writeValueAsString(event)
        );
    }

    private Account lockOne(Long id) {
        return accounts.findAllByIdForUpdate(List.of(id)).stream().findFirst()
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
