package com.ledger.account.application;

import com.ledger.account.domain.Account;
import com.ledger.account.domain.AccountRepository;
import com.ledger.common.domain.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditAccountUseCase {
    private final AccountRepository accountRepository;
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditResult execute(UUID accountId, Money amount, UUID sagaId) {
        log.info("Iniciando crédito. accountId={}, amount={}, sagaId={}", accountId, amount, sagaId);
        Account account = accountRepository
                .findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                    "Conta não encontrada: " + accountId
                )); 
        account.credit(amount);
        accountRepository.save(account);
        log.info("Crédito realizado com sucesso. accountId={}, novoSaldo={}",
                accountId, account.getBalanceAsMoney());
        return CreditResult.success(account.getAccountId(), account.getBalanceAsMoney());
    }
}