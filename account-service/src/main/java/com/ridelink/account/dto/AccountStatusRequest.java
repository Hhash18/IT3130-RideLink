package com.ridelink.account.dto;

import com.ridelink.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AccountStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}