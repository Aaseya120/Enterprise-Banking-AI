package com.bank.customer.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String customerId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;

    /**
     * AES-256-GCM ciphertext (see CryptoUtil), never plaintext. This column
     * intentionally holds only ciphertext -- the entity has no knowledge of
     * encryption keys or algorithms; CustomerService owns encrypt-on-write
     * and decrypt-on-read, keeping this class a plain data holder.
     */
    @Column(name = "national_id_encrypted")
    private String nationalIdEncrypted;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Customer() {
        // JPA
    }

    public Customer(String fullName, String email, String phoneNumber) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.kycStatus = KycStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateKycStatus(KycStatus status) {
        this.kycStatus = status;
        this.updatedAt = Instant.now();
    }

    public void setNationalIdEncrypted(String nationalIdEncrypted) {
        this.nationalIdEncrypted = nationalIdEncrypted;
        this.updatedAt = Instant.now();
    }

    public String getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public KycStatus getKycStatus() { return kycStatus; }
    public String getNationalIdEncrypted() { return nationalIdEncrypted; }
    public boolean hasNationalId() { return nationalIdEncrypted != null; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
