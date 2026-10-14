package com.kevin.transactionPrac.Transactions.controllers;


import com.kevin.transactionPrac.Transactions.dto.AccountResponse;
import com.kevin.transactionPrac.Transactions.dto.CreateAccountRequest;
import com.kevin.transactionPrac.Transactions.dto.TransactionResponse;
import com.kevin.transactionPrac.Transactions.services.AccountService;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.create(request));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(accountService.get(id));
    }

    @GetMapping("/{id}/transactions")
    public Page<TransactionResponse> history(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {

        accountService.get(id);
        return transactionService.history(id, pageable).map(TransactionResponse::from);
    }
}
