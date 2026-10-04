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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current account retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse getCurrentAccount(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return accountService.getAccountById(principal.accountId());
    }

    @PutMapping("/me")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account profile updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "An account with this email already exists")
    })
    public AccountResponse updateCurrentAccount(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateAccountRequest request) {

        return accountService.updateAccount(
                principal.accountId(),
                request
        );
    }

    @PutMapping("/me/password")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect or authentication is required"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current account status updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
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