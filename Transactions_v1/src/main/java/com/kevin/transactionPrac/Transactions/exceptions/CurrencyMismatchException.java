package com.kevin.transactionPrac.Transactions.exceptions;

public class CurrencyMismatchException extends TransactionRejectedException {

    public CurrencyMismatchException(String message) {

//        message = "Currency mismatch between accounts";
        super(message);
    }
}
