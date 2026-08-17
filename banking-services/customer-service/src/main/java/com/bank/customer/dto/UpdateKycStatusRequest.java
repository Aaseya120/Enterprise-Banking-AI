package com.bank.customer.dto;

import com.bank.customer.domain.KycStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateKycStatusRequest(@NotNull KycStatus kycStatus) {
}
