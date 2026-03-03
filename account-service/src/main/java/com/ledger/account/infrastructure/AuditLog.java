package com.ledger.account.infrastructure;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", columnDefinition = "uuid")
    private UUID sagaId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "account_id", columnDefinition = "uuid")
    private UUID accountId;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 3)
    private String currency;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false, length = 64)
    private String checksum;

    public static AuditLog create(UUID sagaId, String eventType,
                                   UUID accountId, BigDecimal amount,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   String currency, String metadata) {
        AuditLog log = new AuditLog();
        log.sagaId = sagaId;
        log.eventType = eventType;
        log.accountId = accountId;
        log.amount = amount;
        log.balanceBefore = balanceBefore;
        log.balanceAfter = balanceAfter;
        log.currency = currency;
        log.metadata = metadata;
        log.occurredAt = OffsetDateTime.now();
        log.checksum = computeChecksum(sagaId, eventType, accountId,
                                  amount, balanceBefore, balanceAfter, log.occurredAt);
        return log;
    }
    //sha-256 do conteúdo - se alguém alterar qualquer campo, o checksum não vai mais bater
    private static String computeChecksum(UUID sagaId, String eventType,
                                           UUID accountId, BigDecimal amount,
                                           BigDecimal balanceBefore, BigDecimal balanceAfter,
                                           OffsetDateTime occurredAt) {
        try {
            String content = String.join("|",
                String.valueOf(sagaId),
                eventType,
                String.valueOf(accountId),
                String.valueOf(amount),
                String.valueOf(balanceBefore),
                String.valueOf(balanceAfter),
                occurredAt.toString()
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao computar checksum do Audit Log", e);
        }
    }
    public boolean isIntegrityValid() {
        String expected = computeChecksum(sagaId, eventType, accountId,
                amount, balanceBefore, balanceAfter, occurredAt);
        return expected.equals(this.checksum);
    }
}