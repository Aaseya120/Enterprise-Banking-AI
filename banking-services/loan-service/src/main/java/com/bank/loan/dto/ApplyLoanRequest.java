package com.bank.loan.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request to apply for a loan. All monetary values are in the specified currency.
 *
 * @param customerId         the applying customer's ID
 * @param accountId          account into which disbursement will be made
 * @param loanType           PERSONAL, HOME, AUTO, EDUCATION
 * @param principalAmount    requested loan amount (> 0)
 * @param annualInterestRate annual rate in percent (e.g. 12.5 for 12.5%)
 * @param tenureMonths       repayment period in months (1–360)
 * @param currency           ISO 4217 currency code
 * @param purpose            free-text description of loan purpose
 * @param creditScore        applicant's credit score (300–850); if absent the
 *                           eligibility check is skipped (demo shortcut only)
 */
public record ApplyLoanRequest(
        @NotBlank String customerId,
        @NotBlank String accountId,
        @NotBlank String loanType,
        @NotNull @DecimalMin("1.00") BigDecimal principalAmount,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal annualInterestRate,
        @Min(1) @Max(360) int tenureMonths,
        @NotBlank String currency,
        @NotBlank String purpose,
        @Min(300) @Max(850) Integer creditScore   // optional – null = skip check
) {}
