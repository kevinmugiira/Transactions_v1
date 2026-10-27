package com.kevin.transactionPrac.Transactions.exceptions;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long id) {

        super("Transaction " + id + " not found!");
    }
}
