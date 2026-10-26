package com.kevin.transactionPrac.Transactions.models;

//import javax;
import com.kevin.transactionPrac.Transactions.exceptions.InsufficientFundsException;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Getter
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user that owns this account: the "sub" claim of their JWT (a UUID from the auth service) */
    @Column(name = "owner_id", length = 36)
    private String ownerId;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "KES";

    // optimistic locking safety net. A stale update fails instead of silently overwriting.
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ensures only hibernate can initialize a new account, callers cannot
    protected Account() {}

    /*
      - Ensures that manual creation of a new account
        can only be initialized with a name and currency
        the rest is added automatically.
      - A user can therefore not create an account with a custom id or balance
     */
    public Account(String ownerId, String ownerName, String currency) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.currency = currency;
    }

    public boolean isOwnedBy(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) throw new InsufficientFundsException(id);
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }
}
