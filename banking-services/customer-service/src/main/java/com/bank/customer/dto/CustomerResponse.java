package com.bank.customer.dto;

import com.bank.customer.domain.Customer;
import com.bank.customer.domain.KycStatus;

import java.time.Instant;

public record CustomerResponse(
        String customerId,
        String fullName,
        String email,
        String phoneNumber,
        KycStatus kycStatus,
        /** Never the actual value (encrypted or plaintext) -- just whether one is on file. See GET /{id}/national-id for the RBAC-gated decrypted read. */
        boolean hasNationalIdOnFile,
        Instant createdAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(), customer.getFullName(), customer.getEmail(),
                customer.getPhoneNumber(), customer.getKycStatus(), customer.hasNationalId(),
                customer.getCreatedAt());
    }
}
