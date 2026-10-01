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
import com.ridelink.account.dto.AccountStatusRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearerAuth")
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

    @PatchMapping("/me/status")
    public ResponseEntity<AccountResponse> updateCurrentAccountStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody AccountStatusRequest request) {

        AccountResponse response = accountService.updateAccountStatus(
                principal.accountId(),
                request.getStatus()
        );

        return ResponseEntity.ok(response);
    }
}