package com.kevin.transactionPrac.Transactions.exceptions;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Account " + id + " not found!");
    }
}
