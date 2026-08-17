package com.bank.customer;

import com.bank.common.exception.BusinessException;
import com.bank.customer.application.CustomerService;
import com.bank.customer.domain.KycStatus;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.NationalIdResponse;
import com.bank.customer.dto.RegisterCustomerRequest;
import com.bank.customer.dto.UpdateKycStatusRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CustomerServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = "banking.audit.events")
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersCustomerAndUpdatesKycStatus() {
        CustomerResponse registered = customerService.register(
                new RegisterCustomerRequest("Jane Doe", "jane.doe@example.com", "+15551234567", null));

        assertThat(registered.kycStatus()).isEqualTo(KycStatus.PENDING);

        CustomerResponse verified = customerService.updateKycStatus(
                registered.customerId(), new UpdateKycStatusRequest(KycStatus.VERIFIED));

        assertThat(verified.kycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void rejectsDuplicateEmail() {
        customerService.register(new RegisterCustomerRequest("A", "dup@example.com", "+15550000001", null));
        assertThatThrownBy(() -> customerService.register(
                new RegisterCustomerRequest("B", "dup@example.com", "+15550000002", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registeredNationalIdIsNeverExposedByGetCustomerAndCanBeDecryptedOnlyByAuthorizedRole() {
        CustomerResponse registered = customerService.register(
                new RegisterCustomerRequest("Sam Lee", "sam.lee@example.com", "+15550009999", "123-45-6789"));

        assertThat(registered.hasNationalIdOnFile()).isTrue();

        CustomerResponse fetched = customerService.getCustomer(registered.customerId());
        assertThat(fetched.hasNationalIdOnFile()).isTrue();
        // CustomerResponse has no field capable of holding the value at all -- structurally
        // impossible to leak it through this DTO, not just "happens not to be populated".

        authenticateAs("COMPLIANCE_OFFICER");
        NationalIdResponse decrypted = customerService.getNationalId(registered.customerId());
        assertThat(decrypted.nationalId()).isEqualTo("123-45-6789");
    }

    @Test
    void getNationalIdIsDeniedWithoutTheRequiredRole() {
        CustomerResponse registered = customerService.register(
                new RegisterCustomerRequest("Alex Kim", "alex.kim@example.com", "+15550001111", "987-65-4321"));

        authenticateAs("CUSTOMER");
        assertThatThrownBy(() -> customerService.getNationalId(registered.customerId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
