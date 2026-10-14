package com.kevin.transactionPrac.Transactions.dto;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long accountId) {

        super("Insufficient funds in account " + accountId);
    }
}
