package com.ridelink.account.controller;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.security.JwtAuthenticationFilter.JwtPrincipal;
import com.ridelink.account.service.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ridelink.account.dto.UpdateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import com.ridelink.account.dto.ChangePasswordRequest;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountResponse getCurrentAccount(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return accountService.getAccountById(principal.accountId());
    }

    @PutMapping("/me")
    public AccountResponse updateCurrentAccount(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateAccountRequest request) {

        return accountService.updateAccount(
                principal.accountId(),
                request
        );
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        accountService.changePassword(
                principal.accountId(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}