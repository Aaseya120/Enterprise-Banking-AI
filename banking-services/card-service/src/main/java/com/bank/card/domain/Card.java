package com.bank.card.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Random;


/**
 * Deliberately never persists a full PAN (plan section 34: "Never log ...
 * full card numbers"). issueCard() generates one, returns it once in
 * IssueCardResponse for the caller to relay to the customer/printer, and
 * only the masked form + last4 are stored here -- exactly the tokenization
 * pattern a real PCI-DSS-scoped system would use (with a real vault instead
 * of an in-process generator).
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String cardId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String maskedPan;

    @Column(nullable = false)
    private String last4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Column(nullable = false)
    private String expiryMonthYear;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Card() {
        // JPA
    }

    private Card(String accountId, String customerId, String maskedPan, String last4,
                 CardType cardType, String expiryMonthYear) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.maskedPan = maskedPan;
        this.last4 = last4;
        this.cardType = cardType;
        this.status = CardStatus.PENDING_ACTIVATION;
        this.expiryMonthYear = expiryMonthYear;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Returns [Card entity to persist, full PAN to return once to the caller]. */
    public static IssuedCard issue(String accountId, String customerId, CardType cardType) {
        String fullPan = generatePan();
        String last4 = fullPan.substring(fullPan.length() - 4);
        String masked = "**** **** **** " + last4;
        String expiry = YearMonth.now().plusYears(4).toString(); // YYYY-MM
        Card card = new Card(accountId, customerId, masked, last4, cardType, expiry);
        return new IssuedCard(card, fullPan);
    }

    private static String generatePan() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("4"); // Visa-like prefix for the demo
        for (int i = 0; i < 15; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public void activate() {
        requireStatus(CardStatus.PENDING_ACTIVATION);
        this.status = CardStatus.ACTIVE;
        touch();
    }

    public void freeze() {
        requireStatus(CardStatus.ACTIVE);
        this.status = CardStatus.FROZEN;
        touch();
    }

    public void unfreeze() {
        requireStatus(CardStatus.FROZEN);
        this.status = CardStatus.ACTIVE;
        touch();
    }

    public void block() {
        if (this.status == CardStatus.CLOSED) {
            throw new IllegalStateException("Cannot block a closed card");
        }
        this.status = CardStatus.BLOCKED;
        touch();
    }

    public void close() {
        this.status = CardStatus.CLOSED;
        touch();
    }

    private void requireStatus(CardStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException("Card must be " + expected + " but is " + this.status);
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public String getCardId() { return cardId; }
    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public String getMaskedPan() { return maskedPan; }
    public String getLast4() { return last4; }
    public CardType getCardType() { return cardType; }
    public CardStatus getStatus() { return status; }
    public String getExpiryMonthYear() { return expiryMonthYear; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public record IssuedCard(Card card, String fullPanOnceOnly) {
    }
}
