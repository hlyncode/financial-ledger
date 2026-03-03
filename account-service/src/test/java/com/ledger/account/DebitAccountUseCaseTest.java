package com.ledger.account;

import com.ledger.account.application.DebitAccountUseCase;
import com.ledger.account.application.DebitResult;
import com.ledger.account.domain.Account;
import com.ledger.account.domain.AccountRepository;
import com.ledger.common.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext; 
import org.springframework.test.context.ActiveProfiles;  
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AccountServiceApplication.class, TestConfig.class})
@ActiveProfiles("test") 
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DebitAccountUseCaseTest {
    @Autowired
    private DebitAccountUseCase debitAccountUseCase;
    @Autowired
    private AccountRepository accountRepository;
    private Account conta;

    @BeforeEach
    void setup() {
        conta = Account.create(
                UUID.randomUUID(),
                "ACC-TEST-001",
                Money.ofBRL(new BigDecimal("1000.00"))
        );
        accountRepository.save(conta);
    }
    @Test
    void deveDebitarComSucesso() {
        Money valor = Money.ofBRL(new BigDecimal("200.00"));
        DebitResult result = debitAccountUseCase.execute(conta.getId(), valor, UUID.randomUUID());

        assertThat(result.success()).isTrue();
        assertThat(result.newBalance().getAmount())
                .isEqualByComparingTo(new BigDecimal("800.00"));
    }
    @Test
    void deveRejeitarSaldoInsuficiente() {
        Money valor = Money.ofBRL(new BigDecimal("9999.00"));
        DebitResult result = debitAccountUseCase.execute(conta.getId(), valor, UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("Saldo insuficiente");
    }
    @Test
    void deveSerIdempotente() {
        Money valor = Money.ofBRL(new BigDecimal("100.00"));
        UUID sagaId = UUID.randomUUID();
        DebitResult primeira = debitAccountUseCase.execute(conta.getId(), valor, sagaId);
        DebitResult segunda = debitAccountUseCase.execute(conta.getId(), valor, sagaId);
        
        assertThat(primeira.success()).isTrue();
        assertThat(segunda.success()).isTrue();
    }
}