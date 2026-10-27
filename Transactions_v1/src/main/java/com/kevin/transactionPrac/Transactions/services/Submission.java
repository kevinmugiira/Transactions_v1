package com.kevin.transactionPrac.Transactions.services;

import com.kevin.transactionPrac.Transactions.models.Transaction;


/** Result of accepting a request. replayed = true when the Idempotency-Key was already used. */
public record Submission(Transaction transaction, boolean replayed) {
}
