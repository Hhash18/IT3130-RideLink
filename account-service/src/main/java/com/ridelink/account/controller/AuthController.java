package com.ridelink.account.controller;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AccountResponse response = accountService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}