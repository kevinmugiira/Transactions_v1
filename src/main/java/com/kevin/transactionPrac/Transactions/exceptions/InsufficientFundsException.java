package com.kevin.transactionPrac.Transactions.exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId) {
        super("Insufficient funds for account ID: " + accountId);
    }
}