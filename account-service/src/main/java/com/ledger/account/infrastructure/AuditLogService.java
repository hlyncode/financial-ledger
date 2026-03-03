package com.ledger.account.infrastructure;

import com.ledger.account.domain.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor 
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    public void logDebit(UUID sagaId, Account account,
                          BigDecimal amount, BigDecimal balanceBefore) {
        AuditLog entry = AuditLog.create(
            sagaId,
            "DEBIT_PERFORMED",
            account.getId(),
            amount,
            balanceBefore,
            account.getBalance(),
            account.getCurrency(),
            buildMetadata(sagaId)
        );
        auditLogRepository.save(entry);
        log.debug("Audit log não registrado. sagaId={}, event=DEBIT_PERFORMED", sagaId);
    }
    public void logCredit(UUID sagaId, Account account,
        BigDecimal amount, BigDecimal balanceBefore) {
        AuditLog entry = AuditLog.create(
            sagaId,
            "CREDIT_PERFORMED",
            account.getId(),
            amount,
            balanceBefore,
            account.getBalance(),
            account.getCurrency(),
            buildMetadata(sagaId)
        );
        auditLogRepository.save(entry);
        log.debug("Audit log registrado. sagaId={}, event=CREDIT_PERFORMED", sagaId);
    }
    public void logReversal(UUID sagaId, Account account,
                             BigDecimal amount, BigDecimal balanceBefore) {
        AuditLog entry = AuditLog.create(
            sagaId,
            "DEBIT_REVERSED",
            account.getId(),
            amount,
            balanceBefore,
            account.getBalance(),
            account.getCurrency(),
            buildMetadata(sagaId)
        );
        auditLogRepository.save(entry);
        log.debug("Audit log registrado. sagaId={}, event=DEBIT_REVERSED", sagaId);
    }
    private String buildMetadata(UUID sagaId) {
        return "{\"sagaId\":\"" + sagaId + "\"}"; 
    }
}