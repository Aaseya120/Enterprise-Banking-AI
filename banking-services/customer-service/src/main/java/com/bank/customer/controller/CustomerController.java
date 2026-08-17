package com.bank.customer.controller;

import com.bank.customer.application.CustomerService;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.NationalIdResponse;
import com.bank.customer.dto.RegisterCustomerRequest;
import com.bank.customer.dto.UpdateKycStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.register(request));
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable String customerId) {
        return customerService.getCustomer(customerId);
    }

    /**
     * The only endpoint that ever returns a decrypted nationalId --
     * enforced by @PreAuthorize on CustomerService.getNationalId(), not
     * here, so the restriction holds even if this method is called
     * directly in-process (e.g. from a future batch job) rather than via
     * HTTP.
     */
    @GetMapping("/{customerId}/national-id")
    public NationalIdResponse getNationalId(@PathVariable String customerId) {
        return customerService.getNationalId(customerId);
    }

    @PatchMapping("/{customerId}/kyc-status")
    public CustomerResponse updateKycStatus(@PathVariable String customerId,
                                             @Valid @RequestBody UpdateKycStatusRequest request) {
        return customerService.updateKycStatus(customerId, request);
    }
}
