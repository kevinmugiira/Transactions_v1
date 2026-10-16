package com.kevin.transactionPrac.Transactions;

import com.kevin.transactionPrac.Transactions.dto.CreateAccountRequest;
import com.kevin.transactionPrac.Transactions.dto.InsufficientFundsException;
import com.kevin.transactionPrac.Transactions.models.Account;
import com.kevin.transactionPrac.Transactions.models.Transaction;
import com.kevin.transactionPrac.Transactions.repositories.AccountRepository;
import com.kevin.transactionPrac.Transactions.services.AccountService;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class TransactionsApplicationTests {


	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("8.4");

	@Autowired
    AccountService accountService;

	@Autowired
    TransactionService txnService;

	@Autowired
    AccountRepository accountRepo;


	private Account newAccount(String name, String seed) {
		Account a = accountService.create(new CreateAccountRequest(name, "KSH"));
		txnService.deposit(a.getId(), new BigDecimal(seed), "seed", null);
		return a;
	}

	private BigDecimal balanceOf(Long id) {
		return accountRepo.findById(id).orElseThrow().getBalance();
	}


	@Test
	void parallelTransfersInBothDirectionsKeepTotalConstant() throws Exception {
		Account a = newAccount("Alice", "1000");
		Account b = newAccount("Bob", "1000");

		ExecutorService pool = Executors.newFixedThreadPool(16);
		List<Future<?>> futures = new ArrayList<>();

		for (int w = 0; w < 200; w++) {
			boolean forward = w % 2 == 0;
			futures.add(pool.submit(() -> {
				if (forward) {
					txnService.transfer(a.getId(), b.getId(), new BigDecimal("5"), null, null);
				} else {
					txnService.transfer(b.getId(), a.getId(), new BigDecimal("3"), null, null);
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
		Account c = newAccount("Allan", "100");
		Account d = newAccount("Deny", "0");

        Transaction first = txnService.transfer(c.getId(), d.getId(), new BigDecimal("40"), null, "key-123");
		Transaction second = txnService.transfer(c.getId(), d.getId(), new BigDecimal("40"), null, "key-123");

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(balanceOf(c.getId())).isEqualByComparingTo("60");
		assertThat(balanceOf(d.getId())).isEqualByComparingTo("40");
	}

	@Test
	void overDraftIsRejectedAndNothingChanges() {
		Account a = newAccount("Underson", "50");
		Account b = newAccount("Bramwel", "0");

		assertThatThrownBy(() -> txnService.transfer(a.getId(), b.getId(), new BigDecimal("80"), null, null))
				.isInstanceOf(InsufficientFundsException.class);
		assertThat(balanceOf(a.getId())).isEqualByComparingTo("50");
		assertThat(balanceOf(b.getId())).isEqualByComparingTo("0");
	}

}
