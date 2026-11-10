package com.kevin.transactionPrac.Transactions;


import com.kevin.transactionPrac.Transactions.dto.CreateAccountRequest;
import com.kevin.transactionPrac.Transactions.exceptions.AccountNotFoundException;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.models.TransactionStatus;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.repositories.OutboxRepository;
import com.kevin.transactionPrac.Transactions.repositories.TransactionRepository;
import com.kevin.transactionPrac.Transactions.security.Caller;
import com.kevin.transactionPrac.Transactions.services.AccountService;
import com.kevin.transactionPrac.Transactions.services.Submission;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class AsyncTransactionFlowTest {


    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql/mysql-server:8.0")
            .asCompatibleSubstituteFor("mysql"))
            .withDatabaseName("TXN_PRACTICE")
            .withUsername("dbUsername")
            .withPassword("dbPassword")
            // Force MySQL to skip heavy disk-synchronization constraints on Linux Mint
            .withCommand(
                    "--innodb-flush-method=nosync",
                    "--innodb-use-native-aio=0",
                    "--innodb-doublewrite=0",
                    "--sync-binlog=0"
            )
            // OPTIONAL: Prevent MySQL from running reverse DNS lookup checks inside your local network loop
            .withEnv("MYSQL_ARGUMENTS", "--skip-name-resolve")
    ;

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.0");

    private static final Caller ALICE = new Caller("alice", false);
    private static final Caller BOB = new Caller("bob", false);

    @Autowired
    AccountService accountService;

    @Autowired
    TransactionService txnService;

    @Autowired
    AccountRepository accountRepo;

    @Autowired
    TransactionRepository txnRepo;

    @Autowired
    OutboxRepository outboxRepo;

    /** Polls until the async processor has moved the transaction out of PENDING. */
    private Transaction awaitFinished(Long id) {
        await().atMost(Duration
                .ofSeconds(30))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> txnRepo.findById(id).orElseThrow().getStatus() != TransactionStatus.PENDING)
        ;
        return txnRepo.findById(id).orElseThrow();
    }

    private Account newAccount(Caller owner, String name, String seed) {
        Account account = accountService.create(new CreateAccountRequest(name, "KES"), owner);
        BigDecimal amount = new BigDecimal(seed);
        if (amount.signum() >= 0) {
            Submission deposit = txnService.submitDeposit(owner, account.getId(), amount, seed, null);
            awaitFinished(deposit.transaction().getId());
        }
        return account;
    }

    private BigDecimal balanceOf(Long id) {
        return accountRepo.findById(id).orElseThrow().getBalance();
    }


    @Test
    void transferIsAcceptedAsPendingThenCompletesAndPublishesAnEvent() {
        Account a = newAccount(ALICE, "Alice", "100");
        Account b = newAccount(BOB, "Bob", "0");

        Submission submission = txnService.submitTransfer(ALICE, a.getId(), b.getId(), new BigDecimal("40"), null, null);
        Long id = submission.transaction().getId();

        assertThat(submission.transaction().getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(awaitFinished(id).getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(balanceOf(a.getId())).isEqualByComparingTo("60");
        assertThat(balanceOf(b.getId())).isEqualByComparingTo("40");

        await()
                .atMost(Duration.ofSeconds(30))
                .until(
                        () -> outboxRepo
                                .findAll()
                                .stream()
                                .anyMatch(
                                        e -> e
                                                .getEventType()
                                                .equals("TransactionCompleted") &&
                                                e.getSentAt() != null &&
                                                e.getPayload().contains("\"transctionId\":" + id)
                                        )
                )
        ;
    }

    @Test
    void overdraftEndsAsFailedAndChangesNothing() {
        Account a = newAccount(ALICE, "Alice", "50");
        Account b = newAccount(BOB, "Bob", "0");

        Long id = txnService
                .submitTransfer(
                        ALICE,
                        a.getId(),
                        b.getId(),
                        new BigDecimal("80"),
                        null,
                        null)
                .transaction()
                .getId()
        ;

        Transaction finished = awaitFinished(id);
        assertThat(finished.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(finished.getFailureReason()).contains("Insufficient funds");
        assertThat(balanceOf(a.getId())).isEqualByComparingTo("50");
        assertThat(balanceOf(b.getId())).isEqualByComparingTo("0");
    }

    @Test
    void sameIdempotencyKeyCreatesOnlyOneTransaction() {
        Account a = newAccount(ALICE, "Alice", "100");
        Account b = newAccount(BOB, "Bob", "0");

        Submission first = txnService.submitTransfer(ALICE, a.getId(), b.getId(), new BigDecimal("40"), null, "key-123");
        Submission second = txnService.submitTransfer(ALICE, a.getId(), b.getId(), new BigDecimal("40"), null, "key-123");

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transaction().getId()).isEqualTo(first.transaction().getId());

        awaitFinished(first.transaction().getId());
        assertThat(balanceOf(a.getId())).isEqualByComparingTo("60");
        assertThat(balanceOf(b.getId())).isEqualByComparingTo("40");
    }

    @Test
    void usersCannotMoveMoneyOutOfAccountsTheyDoNotOwn() {
        Account alices = newAccount(ALICE, "Alice", "100");
        Account bobs = newAccount(BOB, "Bob", "0");

        assertThatThrownBy(
                () -> txnService.submitWithdrawal(
                        BOB,
                        alices.getId(),
                        new BigDecimal("10"),
                        null,
                        null
                )
        ).isInstanceOf(AccountNotFoundException.class);

        assertThatThrownBy(
                () -> txnService.submitTransfer(
                        BOB,
                        alices.getId(),
                        bobs.getId(),
                        new BigDecimal("10"),
                        null,
                        null
                )
        ).isInstanceOf(AccountNotFoundException.class);

        assertThatThrownBy(
                () -> txnService.get(
                        alices.getId(),
                        BOB
                )
        ).isInstanceOf(AccountNotFoundException.class);

        assertThat(balanceOf(alices.getId())).isEqualByComparingTo("100");
    }

    @Test
    void manyParallelTransfersInBothDirectionsAllCompleteAndKeepTheTotal() {
        Account a = newAccount(ALICE, "Alice", "1000");
        Account b = newAccount(BOB, "Bob", "1000");

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Submission s = (i % 2 == 0)
                    ? txnService.submitTransfer(
                            ALICE,
                            a.getId(),
                            b.getId(),
                            new BigDecimal("5"),
                            null,
                            null)
                    : txnService.submitTransfer(
                            BOB,
                            b.getId(),
                            a.getId(),
                            new BigDecimal("3"),
                            null,
                            null);
                    ids.add(s.transaction().getId());
        }
        for (Long id : ids) {
            assertThat(awaitFinished(id).getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        }

        // A: 1000 - 100*5 + 100*3 = 800   B: 1000 + 100*5 - 100*3 = 1200
        assertThat(balanceOf(a.getId())).isEqualByComparingTo("800");
        assertThat(balanceOf(b.getId())).isEqualByComparingTo("1200");
    }

}
