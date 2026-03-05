package com.ledger.reserve.application;

import com.ledger.reserve.domain.Reserve;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class ProcessReserveUseCase {

    public Reserve createReserve(UUID sagaId, UUID targetAccountId, BigDecimal amount, String currency) {
        log.info("Creating reserve. sagaId={}, targetAccountId={}, amount={}, currency={}",
                sagaId, targetAccountId, amount, currency);

        Reserve reserve = new Reserve(UUID.randomUUID(), sagaId, targetAccountId, amount, currency);

        log.info("Reserve created successfully. reserveId={}, status={}", reserve.getId(), reserve.getStatus());
        return reserve;
    }

    public Reserve confirmReserve(Reserve reserve) {
        if (!reserve.isPending()) {
            log.warn("Reserve is not pending. reserveId={}, status={}", reserve.getId(), reserve.getStatus());
            throw new IllegalStateException("Reserve is not in pending state");
        }

        log.info("Confirming reserve. reserveId={}, amount={}, targetAccountId={}",
                reserve.getId(), reserve.getAmount(), reserve.getTargetAccountId());

        reserve.confirm();

        log.info("Reserve confirmed successfully. reserveId={}", reserve.getId());
        return reserve;
    }

    public Reserve cancelReserve(Reserve reserve) {
        if (!reserve.isPending()) {
            log.warn("Reserve is not pending. reserveId={}, status={}", reserve.getId(), reserve.getStatus());
            throw new IllegalStateException("Reserve is not in pending state");
        }

        log.info("Cancelling reserve. reserveId={}", reserve.getId());
        reserve.cancel();

        log.info("Reserve cancelled successfully. reserveId={}", reserve.getId());
        return reserve;
    }
}
