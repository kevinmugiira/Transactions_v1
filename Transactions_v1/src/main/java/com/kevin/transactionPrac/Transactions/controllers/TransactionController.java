package com.kevin.transactionPrac.Transactions.controllers;


import com.kevin.transactionPrac.Transactions.dto.MoneyRequest;
import com.kevin.transactionPrac.Transactions.dto.TransactionResponse;
import com.kevin.transactionPrac.Transactions.dto.TransferRequest;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.security.Caller;
import com.kevin.transactionPrac.Transactions.services.Submission;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
            @RequestHeader(value = "IDEMPOTENCY_HEADER", required = false) String key,
            @AuthenticationPrincipal Jwt jwt) {
        Caller caller = Caller.from(jwt);
        return execute(caller, key, () -> service.submitDeposit(caller, req.accountId(), req.amount(), req.description(), key));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody MoneyRequest req,
            @RequestHeader(value = "IDEMPOTENCY_HEADER", required = false) String key,
            @AuthenticationPrincipal Jwt jwt) {
        Caller caller = Caller.from(jwt);
        return execute(caller, key, () -> service.submitWithdrawal(caller, req.accountId(), req.amount(), req.description(), key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest req,
            @RequestHeader(value = "IDEMPOTENCY_HEADER", required = false) String key,
            @AuthenticationPrincipal Jwt jwt) {
        Caller caller = Caller.from(jwt);
        return execute(caller, key, () -> service.submitTransfer(caller, req.sourceId(), req.targetId(), req.amount(), req.description(), key));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id,
                                   @AuthenticationPrincipal Jwt jwt) {
        return TransactionResponse.from(service.get(id, Caller.from(jwt)));
    }


    /*
      If two requests with the same idempotency key passes the 'what has already been processed' check,
      the loser of the two hits the UNIQUE constraint on idempotency key.
      This has already triggered a rollback, so we get the winner's result and return that instead of an error.
     */
    private ResponseEntity<TransactionResponse> execute(Caller caller, String key, Supplier<Submission> operation) {
        if (key != null && key.length() > 64) {
            throw new IllegalArgumentException(IDEMPOTENCY_HEADER + " must be at most 64 characters");
        }
        Submission submission;

        try {
            submission = operation.get();
//            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(operation.get()));
        } catch (DataIntegrityViolationException e) {
            if (key == null) throw e;
            submission = new Submission(service.findByIdempotencyKey(caller, key).orElseThrow(() -> e), true);

            TransactionResponse body = TransactionResponse.from(submission.transaction());
            if (submission.replayed()) {
                return ResponseEntity.ok(body);
            }
            return ResponseEntity.accepted().location(URI.create("/api/transactions/" + body.id())).body(body);

//            return service.findByIdempotencyKey(key)
//                    .map(tx -> ResponseEntity.ok(TransactionResponse.from(tx)))
//                    .orElseThrow(() -> e);
        }
    }
}
