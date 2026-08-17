package com.bank.card.application;

import com.bank.card.domain.Card;
import com.bank.card.domain.CardRepository;
import com.bank.card.dto.CardResponse;
import com.bank.card.dto.IssueCardRequest;
import com.bank.common.events.AuditEventPublisher;
import com.bank.common.exception.BusinessException;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Card lifecycle management. Audit emission (gap filled): CARD_ISSUED,
 * CARD_BLOCKED, and CARD_CLOSED are published to banking.audit.events —
 * these are the highest-risk card events that require an audit trail.
 * Freeze/unfreeze/activate are lower-risk and not audited to keep the
 * trail signal-to-noise ratio high.
 */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AuditEventPublisher auditPublisher;

    public CardService(CardRepository cardRepository,
                       AuditEventPublisher auditPublisher) {
        this.cardRepository = cardRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public IssueCardRequest.Response issueCard(IssueCardRequest request) {
        Card.IssuedCard issued = Card.issue(request.accountId(), request.customerId(), request.cardType());
        cardRepository.save(issued.card());
        auditPublisher.publish(
                MDC.get("userId"), "CARD_ISSUED",
                "Card/" + issued.card().getCardId(),
                true, "card-service",
                Map.of("accountId", request.accountId(),
                        "customerId", request.customerId(),
                        "cardType", request.cardType().name()));
        return IssueCardRequest.Response.from(issued);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(String cardId) {
        return CardResponse.from(findOrThrow(cardId));
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getCardsForAccount(String accountId) {
        return cardRepository.findByAccountId(accountId).stream().map(CardResponse::from).toList();
    }

    @Transactional
    public CardResponse activate(String cardId) {
        return applyTransition(cardId, Card::activate);
    }

    @Transactional
    public CardResponse freeze(String cardId) {
        return applyTransition(cardId, Card::freeze);
    }

    @Transactional
    public CardResponse unfreeze(String cardId) {
        return applyTransition(cardId, Card::unfreeze);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('BANK_STAFF','ADMIN')")
    public CardResponse block(String cardId) {
        CardResponse response = applyTransition(cardId, Card::block);
        auditPublisher.publish(
                MDC.get("userId"), "CARD_BLOCKED",
                "Card/" + cardId,
                true, "card-service",
                Map.of("cardId", cardId));
        return response;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('BANK_STAFF','ADMIN')")
    public CardResponse close(String cardId) {
        CardResponse response = applyTransition(cardId, Card::close);
        auditPublisher.publish(
                MDC.get("userId"), "CARD_CLOSED",
                "Card/" + cardId,
                true, "card-service",
                Map.of("cardId", cardId));
        return response;
    }

    private CardResponse applyTransition(String cardId, java.util.function.Consumer<Card> transition) {
        Card card = findOrThrow(cardId);
        try {
            transition.accept(card);
        } catch (IllegalStateException e) {
            throw BusinessException.ruleViolation(e.getMessage());
        }
        return CardResponse.from(cardRepository.save(card));
    }

    private Card findOrThrow(String cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> BusinessException.notFound("Card not found: " + cardId));
    }
}
