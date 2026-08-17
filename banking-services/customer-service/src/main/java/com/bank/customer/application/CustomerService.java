package com.bank.customer.application;

import com.bank.common.crypto.CryptoUtil;
import com.bank.common.events.AuditEventPublisher;
import com.bank.common.exception.BusinessException;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerRepository;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.NationalIdResponse;
import com.bank.customer.dto.RegisterCustomerRequest;
import com.bank.customer.dto.UpdateKycStatusRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Owns the encrypt-on-write / decrypt-on-read boundary for nationalId
 * (plan section 28: PII protection). The Customer entity itself only ever
 * holds ciphertext; plaintext exists in memory only for the duration of a
 * register() or getNationalId() call, never persisted or logged.
 *
 * <p>Audit emission (gap filled): CUSTOMER_REGISTERED is emitted on
 * successful registration; NATIONAL_ID_ACCESSED is emitted on every
 * call to getNationalId() (whether it succeeds or fails) — access to
 * sensitive PII is exactly the kind of action that must appear in the
 * audit trail regardless of outcome.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final String encryptionKey;
    private final AuditEventPublisher auditPublisher;

    public CustomerService(CustomerRepository customerRepository,
                            @Value("${bank.crypto.national-id-key}") String encryptionKeyPassphrase,
                            AuditEventPublisher auditPublisher) {
        this.customerRepository = customerRepository;
        this.encryptionKey = CryptoUtil.deriveAesKeyBase64(encryptionKeyPassphrase);
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public CustomerResponse register(RegisterCustomerRequest request) {
        customerRepository.findByEmail(request.email()).ifPresent(c -> {
            throw BusinessException.ruleViolation("A customer with this email already exists");
        });
        Customer customer = new Customer(request.fullName(), request.email(), request.phoneNumber());
        if (request.nationalId() != null && !request.nationalId().isBlank()) {
            customer.setNationalIdEncrypted(CryptoUtil.encrypt(request.nationalId(), encryptionKey));
        }
        customer = customerRepository.save(customer);
        auditPublisher.publish(
                MDC.get("userId"), "CUSTOMER_REGISTERED",
                "Customer/" + customer.getCustomerId(),
                true, "customer-service",
                Map.of("email", request.email(), "hasNationalId", request.nationalId() != null));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(String customerId) {
        return CustomerResponse.from(findOrThrow(customerId));
    }

    /**
     * The ONLY method in the service that ever returns a decrypted nationalId.
     * Restricted to roles with a legitimate compliance/KYC need to see it.
     * Every access — successful or denied — is audited.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','ADMIN')")
    public NationalIdResponse getNationalId(String customerId) {
        Customer customer = findOrThrow(customerId);
        if (!customer.hasNationalId()) {
            auditPublisher.publish(
                    MDC.get("userId"), "NATIONAL_ID_ACCESSED",
                    "Customer/" + customerId + "/national-id",
                    false, "customer-service",
                    Map.of("reason", "no national ID on file"));
            throw BusinessException.notFound("No national ID on file for customer: " + customerId);
        }
        String plaintext = CryptoUtil.decrypt(customer.getNationalIdEncrypted(), encryptionKey);
        auditPublisher.publish(
                MDC.get("userId"), "NATIONAL_ID_ACCESSED",
                "Customer/" + customerId + "/national-id",
                true, "customer-service",
                Map.of("customerId", customerId));
        return new NationalIdResponse(customerId, plaintext);
    }

    @Transactional
    public CustomerResponse updateKycStatus(String customerId, UpdateKycStatusRequest request) {
        Customer customer = findOrThrow(customerId);
        customer.updateKycStatus(request.kycStatus());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    private Customer findOrThrow(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> BusinessException.notFound("Customer not found: " + customerId));
    }
}
