package com.kevin.transactionPrac.Transactions.services;


import com.kevin.transactionPrac.Transactions.dto.TransferRequest;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionStatus;
import com.kevin.transactionPrac.Transactions.models.TransactionType;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

//@Service
//@RequiredArgsConstructor
public class TransferService {

//    private final AccountRepository accounts;
//    private final TransactionRepository transactions;
//
//    @Transactional
//    public Transaction transfer(TransferRequest req, String idempotencyKey) throws AccountNotFoundException {
//
//        //ensure the idempotency key is present and then retrieve it
//        if (idempotencyKey != null) {
//            var existing = transactions.findByIndempotencyKey(idempotencyKey);
//            if (existing.isPresent()) return existing.get();
//        }
//
//        if (req.sourceId().equals(req.targetId())) {
//            throw new IllegalArgumentException("Cannot transfer to the same account");
//        }
//
//        var locked = accounts.findAllByIdForUpdate(List.of(req.sourceId(), req.targetId()));
//        if (locked.size() != 2) throw new AccountNotFoundException();
//
//        Account source = (Account) locked.stream().filter(a -> a.getId().equals(req.sourceId()));
//        Account target = (Account) locked.stream().filter(a -> a.getId().equals(req.targetId()));
//
//        source.debit(req.amount());
//        target.credit(req.amount());
//
//        Transaction tx = new Transaction();
//        tx.setType(TransactionType.TRANSFER);
//        tx.setStatus(TransactionStatus.COMPLETED);
//        tx.setAmount(req.amount());
//        tx.setSourceAccountId(source.getId());
//        tx.setTargetAccountId(target.getId());
//        tx.setIdempotency_key(idempotencyKey);
//        return transactions.save(tx);
//    }
}
