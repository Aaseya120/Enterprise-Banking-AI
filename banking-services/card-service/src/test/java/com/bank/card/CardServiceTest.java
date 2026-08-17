package com.bank.card;

import com.bank.card.application.CardService;
import com.bank.card.domain.CardStatus;
import com.bank.card.domain.CardType;
import com.bank.card.dto.IssueCardRequest;
import com.bank.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CardServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = "banking.audit.events")
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Test
    void issuedCardNeverExposesFullPanAfterCreation() {
        IssueCardRequest.Response issued = cardService.issueCard(
                new IssueCardRequest("ACC-1", "CUST-1", CardType.DEBIT));

        assertThat(issued.fullPanOnceOnly()).hasSize(16);
        assertThat(issued.status()).isEqualTo(CardStatus.PENDING_ACTIVATION);

        var fetched = cardService.getCard(issued.cardId());
        assertThat(fetched.maskedPan()).startsWith("**** **** **** ");
        assertThat(fetched.maskedPan()).doesNotContain(issued.fullPanOnceOnly());
    }

    @Test
    void lifecycleTransitionsEnforceValidStates() {
        IssueCardRequest.Response issued = cardService.issueCard(
                new IssueCardRequest("ACC-2", "CUST-2", CardType.CREDIT));

        assertThatThrownBy(() -> cardService.freeze(issued.cardId())).isInstanceOf(BusinessException.class);

        cardService.activate(issued.cardId());
        var frozen = cardService.freeze(issued.cardId());
        assertThat(frozen.status()).isEqualTo(CardStatus.FROZEN);

        var unfrozen = cardService.unfreeze(issued.cardId());
        assertThat(unfrozen.status()).isEqualTo(CardStatus.ACTIVE);
    }
}
