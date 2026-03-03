package com.ledger.account.application;

import com.ledger.account.domain.Account;
import com.ledger.account.domain.AccountRepository;
import com.ledger.account.domain.InsufficientFundsException;
import com.ledger.account.infrastructure.AuditLogService;
import com.ledger.common.domain.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitAccountUseCase {
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DebitResult execute(UUID accountId, Money amount, UUID sagaId) {
        log.info("Iniciando débito. accountId={}, amount={}, sagaId={}", accountId, amount, sagaId);
        Account account = accountRepository
                .findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Conta não encontrada: " + accountId));
        try {
            //captura saldo antes do débito para o Audit Log
            BigDecimal balanceBefore = account.getBalance();
            account.debit(amount);
            accountRepository.save(account);
            //registra no Audit Log — mesma transação, atomicidade garantida
            auditLogService.logDebit(sagaId, account, amount.getAmount(), balanceBefore);
            log.info("Débito realizado. accountId={}, novoSaldo={}", accountId, account.getBalanceAsMoney());
            return DebitResult.success(account.getAccountId(), account.getBalanceAsMoney());
        } catch (InsufficientFundsException e) {
            log.warn("Saldo insuficiente. accountId={}, sagaId={}", accountId, sagaId);
            return DebitResult.failure(e.getMessage());
        }
    }
}