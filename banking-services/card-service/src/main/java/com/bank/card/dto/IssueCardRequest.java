package com.bank.card.dto;

import com.bank.card.domain.Card;
import com.bank.card.domain.CardStatus;
import com.bank.card.domain.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record IssueCardRequest(
        @NotBlank String accountId,
        @NotBlank String customerId,
        @NotNull CardType cardType
) {
    public record Response(
            String cardId,
            String maskedPan,
            String fullPanOnceOnly,
            CardType cardType,
            CardStatus status,
            String expiryMonthYear,
            Instant createdAt
    ) {
        public static Response from(Card.IssuedCard issuedCard) {
            Card card = issuedCard.card();
            return new Response(card.getCardId(), card.getMaskedPan(), issuedCard.fullPanOnceOnly(),
                    card.getCardType(), card.getStatus(), card.getExpiryMonthYear(), card.getCreatedAt());
        }
    }
}
