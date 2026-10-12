package com.kevin.transactionPrac.Transactions.models;

//import javax;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner_name;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    private String currency = "KSH";

    private Long version;

    private Instant createdAt = Instant.now();

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) throw new InsufficientFundsException(id);
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }
}
