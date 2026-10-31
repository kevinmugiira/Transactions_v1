package com.kevin.transactionPrac.Transactions.controllers;


import com.kevin.transactionPrac.Transactions.dto.AccountResponse;
import com.kevin.transactionPrac.Transactions.dto.CreateAccountRequest;
import com.kevin.transactionPrac.Transactions.dto.TransactionResponse;
import com.kevin.transactionPrac.Transactions.security.Caller;
import com.kevin.transactionPrac.Transactions.services.AccountService;
import com.kevin.transactionPrac.Transactions.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.create(request, Caller.from(jwt)));
    }

    @GetMapping
    public List<AccountResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return accountService.listMine(Caller.from(jwt)).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id,
                               @AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.get(id, Caller.from(jwt)));
    }

    @GetMapping("/{id}/transactions")
    public Page<TransactionResponse> history(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {

        return transactionService.history(id, Caller.from(jwt), pageable).map(TransactionResponse::from);
    }
}
