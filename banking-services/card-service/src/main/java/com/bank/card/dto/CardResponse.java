package com.bank.card.dto;

import com.bank.card.domain.Card;
import com.bank.card.domain.CardStatus;
import com.bank.card.domain.CardType;

import java.time.Instant;

public record CardResponse(
        String cardId,
        String accountId,
        String customerId,
        String maskedPan,
        CardType cardType,
        CardStatus status,
        String expiryMonthYear,
        Instant createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(card.getCardId(), card.getAccountId(), card.getCustomerId(),
                card.getMaskedPan(), card.getCardType(), card.getStatus(), card.getExpiryMonthYear(),
                card.getCreatedAt());
    }
}
