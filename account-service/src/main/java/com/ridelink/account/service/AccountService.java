package com.ridelink.account.service;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.entity.Account;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.exception.AccountNotFoundException;
import com.ridelink.account.exception.AdminRegistrationException;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ridelink.account.dto.UpdateAccountRequest;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountService(AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {

        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AccountResponse register(RegisterRequest request) {

        if (request.getRole() == Role.ADMIN) {
            throw new AdminRegistrationException(
                    "Admin accounts cannot be created through public registration"
            );
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "An account with this email already exists"
            );
        }

        Account account = new Account();

        account.setName(request.getName());
        account.setEmail(request.getEmail());
        account.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        account.setRole(request.getRole());

        Account savedAccount = accountRepository.save(account);

        return mapToResponse(savedAccount);
    }

    public LoginResponse login(LoginRequest request) {

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"
                ));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Account is not active"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                account.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(account);

        LoginResponse response = new LoginResponse();

        response.setToken(token);
        response.setAccountId(account.getId());
        response.setEmail(account.getEmail());
        response.setRole(account.getRole());

        return response;
    }

    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return mapToResponse(account);
    }

    public AccountResponse updateAccount(
            UUID accountId,
            UpdateAccountRequest request) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account not found"));

        if (!account.getEmail().equalsIgnoreCase(request.getEmail())
                && accountRepository.existsByEmail(request.getEmail())) {

            throw new DuplicateEmailException(
                    "An account with this email already exists"
            );
        }

        account.setName(request.getName());
        account.setEmail(request.getEmail());

        Account updatedAccount = accountRepository.save(account);

        return mapToResponse(updatedAccount);
    }

    private AccountResponse mapToResponse(Account account) {

        AccountResponse response = new AccountResponse();

        response.setId(account.getId());
        response.setName(account.getName());
        response.setEmail(account.getEmail());
        response.setRole(account.getRole());
        response.setStatus(account.getStatus());
        response.setCreatedAt(account.getCreatedAt());

        return response;
    }
}

