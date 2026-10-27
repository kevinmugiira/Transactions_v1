package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.dto.CreateAccountRequest;
import com.kevin.transactionPrac.Transactions.exceptions.AccountNotFoundException;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.security.Caller;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accounts;

    /** The new account belongs to whoever is calling. The owner is never taken from the request body. */
    @Transactional
    public Account create(CreateAccountRequest req, Caller caller) {
        String currency = req.currency() != null ? req.currency() : "KES";
        return accounts.save(new Account(caller.userId(), req.ownerName(), currency));
    }

    /**
     * Owners and admins can read an account. Anyone else gets "not found", exactly as if it did not
     * exist: account ids are sequential and guessable, so confirming they exist would help an attacker.
     */
    @Transactional(readOnly = true)
    public Account get(Long id, Caller caller) {

        Account account = accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        if (!caller.admin() && !account.isOwnedBy(caller.userId())) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }

    @Transactional(readOnly = true)
    public List<Account> listMine(Caller caller) {
        return accounts.findByOwnerIdOrderByIdAsc(caller.userId());
    }

}
