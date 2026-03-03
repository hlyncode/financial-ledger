package com.ledger.account.api;

import com.ledger.account.infrastructure.AuditLog;
import com.ledger.account.infrastructure.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;

    //consulta todo o histórico de uma conta
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<AuditLogResponse>> getByAccount(
            @PathVariable UUID accountId) {
        log.info("Consultando audit log. accountId={}", accountId);
        List<AuditLogResponse> response = auditLogRepository
                .findByAccountIdOrderByOccurredAtDesc(accountId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // consulta todo o histórico de uma saga específica
    @GetMapping("/sagas/{sagaId}")
    public ResponseEntity<List<AuditLogResponse>> getBySaga(
            @PathVariable UUID sagaId) {
        log.info("Consultando audit log. sagaId={}", sagaId);
        List<AuditLogResponse> response = auditLogRepository
                .findBySagaIdOrderByOccurredAtAsc(sagaId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
    //verifica integridade de todos os registros de uma conta
    @GetMapping("/accounts/{accountId}/integrity")
    public ResponseEntity<IntegrityResponse> checkIntegrity(
            @PathVariable UUID accountId) {
        log.info("Verificando integridade do audit log. accountId={}", accountId);
        List<AuditLog> logs = auditLogRepository
                .findByAccountIdOrderByOccurredAtDesc(accountId);
        long total = logs.size();
        long corrompidos = logs.stream()
                .filter(l -> !l.isIntegrityValid())
                .count();
        return ResponseEntity.ok(new IntegrityResponse(total, corrompidos,
                corrompidos == 0 ? "OK" : "CORROMPIDO"));
    }
} 