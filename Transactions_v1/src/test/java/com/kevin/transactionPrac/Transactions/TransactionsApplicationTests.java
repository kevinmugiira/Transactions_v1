package com.kevin.transactionPrac.Transactions;

import com.kevin.transactionPrac.Transactions.dto.CreateAccountRequest;
//import com.kevin.transactionPrac.Transactions.dto.InsufficientFundsException;
import com.kevin.transactionPrac.Transactions.exceptions.InsufficientFundsException;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.security.Caller;
import com.kevin.transactionPrac.Transactions.services.AccountService;
import com.kevin.transactionPrac.Transactions.services.Submission;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//import static com.kevin.transactionPrac.Transactions.AsyncTransactionFlowTest.ALICE;
//import static com.kevin.transactionPrac.Transactions.AsyncTransactionFlowTest.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Value;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class TransactionsApplicationTests {


	private static final Caller ALICE = new Caller("alice", false);
	private static final Caller BOB = new Caller("bob", false);


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
			.withEnv("MYSQL_ARGUMENTS", "--skip-name-resolve");


	@Autowired
    AccountService accountService;

	@Autowired
    TransactionService txnService;

	@Autowired
    AccountRepository accountRepo;


	private Account newAccount(Caller owner, String name, String seed) {
		Account a = accountService.create(new CreateAccountRequest(name, "KES"), owner);
		BigDecimal amount = new BigDecimal(seed);
		if (amount.signum() > 0) {
			txnService.submitDeposit(owner, a.getId(), amount, "seed", null);
		}
		return a;
	}

	private BigDecimal balanceOf(Long id) {
		return accountRepo.findById(id).orElseThrow().getBalance();
	}


	@Test
	void parallelTransfersInBothDirectionsKeepTotalConstant() throws Exception {
		Account a = newAccount(ALICE,"Alice", "1000");
		Account b = newAccount(BOB,"Bob", "1000");

		ExecutorService pool = Executors.newFixedThreadPool(16);
		List<Future<?>> futures = new ArrayList<>();

		for (int w = 0; w < 200; w++) {
			boolean forward = w % 2 == 0;
			futures.add(pool.submit(() -> {
				if (forward) {
					txnService.submitTransfer(ALICE, a.getId(), b.getId(), new BigDecimal("5"), null, null);
				} else {
					txnService.submitTransfer(BOB, b.getId(), a.getId(), new BigDecimal("3"), null, null);
				}
			}));
		}

		for (Future<?> f : futures) {
			f.get(60, TimeUnit.SECONDS);
		}

		pool.shutdown();

		assertThat(balanceOf(a.getId())).isEqualByComparingTo("800");
		assertThat(balanceOf(b.getId())).isEqualByComparingTo("1200");
		assertThat(balanceOf(a.getId()).add(balanceOf(b.getId()))).isEqualByComparingTo("2000");
	}

	@Test
	void sameIdempotencyKeyOnlyMovesMoneyOnce() {
		Account c = newAccount(ALICE,"Allan", "100");
		Account d = newAccount(BOB,"Deny", "0");

        Submission first = txnService.submitTransfer(ALICE, c.getId(), d.getId(), new BigDecimal("40"), null, "key-123");
		Submission second = txnService.submitTransfer(BOB, c.getId(), d.getId(), new BigDecimal("40"), null, "key-123");

		assertThat(first.replayed()).isFalse();
		assertThat(second.replayed()).isTrue();
//		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(balanceOf(c.getId())).isEqualByComparingTo("60");
		assertThat(balanceOf(d.getId())).isEqualByComparingTo("40");
	}

	@Test
	void overDraftIsRejectedAndNothingChanges() {
		Account a = newAccount(ALICE,"Underson", "50");
		Account b = newAccount(BOB, "Bramwel", "0");

		assertThatThrownBy(() -> txnService.submitTransfer(ALICE, a.getId(), b.getId(), new BigDecimal("80"), null, null))
				.isInstanceOf(com.kevin.transactionPrac.Transactions.exceptions.InsufficientFundsException.class);
		assertThat(balanceOf(a.getId())).isEqualByComparingTo("50");
		assertThat(balanceOf(b.getId())).isEqualByComparingTo("0");
	}

}
