package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.exceptions.AccountNotFoundException;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accounts;

    @Transactional
    public Account create(CreateAccountRequest req) {
        String currency = req.currency() != null ? req.currency() : "KSH";
        return accounts.save(new Account(req.ownerName(), currency));
    }

    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

}
