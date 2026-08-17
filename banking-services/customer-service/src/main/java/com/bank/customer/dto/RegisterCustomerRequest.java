package com.bank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterCustomerRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String phoneNumber,
        /** Optional. Encrypted (AES-256-GCM) before storage -- see CustomerService.register(). Never logged or echoed back in any response. */
        String nationalId
) {
}
