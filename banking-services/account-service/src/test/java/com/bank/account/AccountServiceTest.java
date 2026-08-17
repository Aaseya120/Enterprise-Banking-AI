package com.bank.account;

import com.bank.account.application.AccountService;
import com.bank.account.domain.AccountType;
import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.AdjustBalanceRequest;
import com.bank.account.dto.OpenAccountRequest;
import com.bank.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = AccountServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = "banking.audit.events")
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Test
    void opensAccountAndTracksBalance() {
        AccountResponse opened = accountService.openAccount(
                new OpenAccountRequest("CUST-1", AccountType.SAVINGS, new BigDecimal("1000.00"), "USD"));

        assertThat(opened.balance()).isEqualByComparingTo("1000.00");

        AccountResponse afterCredit = accountService.credit(opened.accountId(),
                new AdjustBalanceRequest(new BigDecimal("250.00"), "test-credit"));
        assertThat(afterCredit.balance()).isEqualByComparingTo("1250.00");

        AccountResponse afterDebit = accountService.debit(opened.accountId(),
                new AdjustBalanceRequest(new BigDecimal("300.00"), "test-debit"));
        assertThat(afterDebit.balance()).isEqualByComparingTo("950.00");
    }

    @Test
    void rejectsOverdraft() {
        AccountResponse opened = accountService.openAccount(
                new OpenAccountRequest("CUST-2", AccountType.SAVINGS, new BigDecimal("100.00"), "USD"));

        assertThatThrownBy(() -> accountService.debit(opened.accountId(),
                new AdjustBalanceRequest(new BigDecimal("500.00"), "test-overdraft")))
                .isInstanceOf(BusinessException.class);
    }
}
