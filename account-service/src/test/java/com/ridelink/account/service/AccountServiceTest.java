package com.ridelink.account.service;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.ChangePasswordRequest;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UpdateAccountRequest;
import com.ridelink.account.entity.Account;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.exception.AccountNotFoundException;
import com.ridelink.account.exception.AdminRegistrationException;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void registerPassengerEncodesPasswordAndReturnsSafeAccountData() {
        UUID accountId = UUID.randomUUID();
        RegisterRequest request = registrationRequest(Role.PASSENGER);
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("PassengerPassword123")).thenReturn("encoded-passenger-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(accountId);
            account.setStatus(AccountStatus.ACTIVE);
            return account;
        });

        AccountResponse response = accountService.register(request);

        ArgumentCaptor<Account> savedAccount = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedAccount.capture());
        assertEquals("encoded-passenger-password", savedAccount.getValue().getPassword());
        assertEquals(accountId, response.getId());
        assertEquals("Morgan Passenger", response.getName());
        assertEquals("morgan.passenger@example.test", response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertFalse(hasPasswordProperty(AccountResponse.class));
    }

    @Test
    void registerDriverPersistsDriverRole() {
        RegisterRequest request = registrationRequest(Role.DRIVER);
            request.setName("Taylor Driver");
            request.setEmail("taylor.driver@example.test");
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("PassengerPassword123")).thenReturn("encoded-driver-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.register(request);

        ArgumentCaptor<Account> savedAccount = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(savedAccount.capture());
        assertEquals(Role.DRIVER, savedAccount.getValue().getRole());
            assertEquals("encoded-driver-password", savedAccount.getValue().getPassword());
        assertEquals(Role.DRIVER, response.getRole());
    }

    @Test
    void registerRejectsDuplicateEmailWithoutSaving() {
        RegisterRequest request = registrationRequest(Role.PASSENGER);
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> accountService.register(request));

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void registerRejectsAdminRoleWithoutSaving() {
        RegisterRequest request = registrationRequest(Role.ADMIN);

        assertThrows(AdminRegistrationException.class, () -> accountService.register(request));

        verifyNoInteractions(accountRepository, passwordEncoder, jwtService);
    }

    @Test
    void loginReturnsAccountIdentityAndGeneratedTokenForActiveAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        LoginRequest request = loginRequest(account.getEmail(), "PassengerPassword123");
        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("PassengerPassword123", "stored-password-hash")).thenReturn(true);
        when(jwtService.generateToken(account)).thenReturn("fictional-test-token");

        LoginResponse response = accountService.login(request);

        assertEquals("fictional-test-token", response.getToken());
        assertEquals(accountId, response.getAccountId());
        assertEquals(account.getEmail(), response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
        verify(jwtService).generateToken(account);
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginRequest request = loginRequest("unknown@example.test", "irrelevant-test-password");
        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(request));

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void loginRejectsWrongPasswordWithoutGeneratingToken() {
        Account account = account(UUID.randomUUID(), "morgan.passenger@example.test", Role.PASSENGER);
        LoginRequest request = loginRequest(account.getEmail(), "WrongPassword123");
        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPassword123", "stored-password-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(request));

        verify(jwtService, never()).generateToken(any(Account.class));
    }

    @Test
    void loginRejectsNonActiveAccountWithoutGeneratingToken() {
        Account account = account(UUID.randomUUID(), "morgan.passenger@example.test", Role.PASSENGER);
        account.setStatus(AccountStatus.SUSPENDED);
        LoginRequest request = loginRequest(account.getEmail(), "PassengerPassword123");
        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> accountService.login(request));

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void getAccountByIdReturnsAccountResponse() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountById(accountId);

        assertEquals(accountId, response.getId());
        assertEquals("Morgan Passenger", response.getName());
        assertEquals("morgan.passenger@example.test", response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertFalse(hasPasswordProperty(AccountResponse.class));
    }

    @Test
    void getAccountByIdThrowsWhenAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(accountId));
    }

    @Test
    void updateAccountChangesNameAndEmailAndSavesAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Morgan Updated");
        request.setEmail("morgan.updated@example.test");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(accountRepository.save(account)).thenReturn(account);

        AccountResponse response = accountService.updateAccount(accountId, request);

        assertEquals("Morgan Updated", account.getName());
        assertEquals("morgan.updated@example.test", account.getEmail());
        assertEquals("Morgan Updated", response.getName());
        assertEquals("morgan.updated@example.test", response.getEmail());
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccountRejectsEmailAlreadyUsedByAnotherAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Morgan Updated");
        request.setEmail("other.account@example.test");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> accountService.updateAccount(accountId, request));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changePasswordEncodesDifferentNewPasswordAndSavesAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("CurrentPassword123");
        request.setNewPassword("DifferentPassword123");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("CurrentPassword123", "stored-password-hash")).thenReturn(true);
        when(passwordEncoder.matches("DifferentPassword123", "stored-password-hash")).thenReturn(false);
        when(passwordEncoder.encode("DifferentPassword123")).thenReturn("encoded-new-password");
        when(accountRepository.save(account)).thenReturn(account);

        accountService.changePassword(accountId, request);

        assertEquals("encoded-new-password", account.getPassword());
        verify(passwordEncoder).encode("DifferentPassword123");
        verify(accountRepository).save(account);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongCurrentPassword123");
        request.setNewPassword("DifferentPassword123");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongCurrentPassword123", "stored-password-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accountService.changePassword(accountId, request));

        verify(accountRepository, never()).save(any(Account.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void updateAccountStatusChangesStatusAndSavesAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, "morgan.passenger@example.test", Role.PASSENGER);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountResponse response = accountService.updateAccountStatus(accountId, AccountStatus.SUSPENDED);

        assertEquals(AccountStatus.SUSPENDED, account.getStatus());
        assertEquals(AccountStatus.SUSPENDED, response.getStatus());
        verify(accountRepository).save(account);
    }

    private static RegisterRequest registrationRequest(Role role) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Morgan Passenger");
        request.setEmail("morgan.passenger@example.test");
        request.setPassword("PassengerPassword123");
        request.setRole(role);
        return request;
    }

    private static LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static Account account(UUID id, String email, Role role) {
        Account account = new Account();
        account.setId(id);
        account.setName("Morgan Passenger");
        account.setEmail(email);
        account.setPassword("stored-password-hash");
        account.setRole(role);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private static boolean hasPasswordProperty(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.getName().equalsIgnoreCase("password"));
    }
}
