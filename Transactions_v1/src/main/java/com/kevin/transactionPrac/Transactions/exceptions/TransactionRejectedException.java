package com.kevin.transactionPrac.Transactions.exceptions;

public class TransactionRejectedException extends RuntimeException {

    protected TransactionRejectedException(String message) {
        super(message);
    }
}
