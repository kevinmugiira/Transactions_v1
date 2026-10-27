package com.kevin.transactionPrac.Transactions.controllers;


import com.kevin.transactionPrac.Transactions.dto.MoneyRequest;
import com.kevin.transactionPrac.Transactions.dto.TransactionResponse;
import com.kevin.transactionPrac.Transactions.dto.TransferRequest;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final String IDEMPOTENCY_HEADER = "idempotency-key";

    private final TransactionService service;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody MoneyRequest req,
            @RequestHeader(value = "IDEMPOTENCY_HEADER", required = false) String key) {
        return execute(key, () -> service.deposit(req.accountId(), req.amount(), req.description(), key));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody MoneyRequest req,
            @RequestHeader(value = "IDEMPOTENCY_HEADER", required = false) String key) {
        return execute(key, () -> service.withdraw(req.accountId(), req.amount(), req.description(), key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest req,
            @RequestHeader(value = "IDEMPOTENCY_HEADER", required = false) String key) {
        return execute(key, () -> service.transfer(req.sourceId(), req.targetId(), req.amount(), req.description(), key));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return TransactionResponse.from(service.get(id));
    }


    /*
      If two requests with the same idempotency key passes the 'what has already been processed' check,
      the loser of the two hits the UNIQUE constraint on idempotency key.
      This has already triggered a rollback, so we get the winner's result and return that instead of an error.
     */
    private ResponseEntity<TransactionResponse> execute(String key, Supplier<Transaction> operation) {
        if (key != null && key.length() > 64) {
            throw new IllegalArgumentException(IDEMPOTENCY_HEADER + " must be at most 64 characters");
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(operation.get()));
        } catch (DataIntegrityViolationException e) {
            if (key == null) throw e;
            return service.findByIdempotencyKey(key)
                    .map(tx -> ResponseEntity.ok(TransactionResponse.from(tx)))
                    .orElseThrow(() -> e);
        }
    }
}
