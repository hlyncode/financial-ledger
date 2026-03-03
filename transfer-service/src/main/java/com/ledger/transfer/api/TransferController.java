package com.ledger.transfer.api;

import com.ledger.transfer.idempotency.IdempotencyService;
import com.ledger.transfer.saga.ResilientSagaService;
import com.ledger.transfer.saga.SagaExecution;
import com.ledger.transfer.saga.SagaUnavailableException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final IdempotencyService idempotencyService;
    private final ResilientSagaService sagaOrchestrator;
    @PostMapping
    public ResponseEntity<?> transfer(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        log.info("Requisição recebida. idempotencyKey={}, from={}, to={}, amount={}",
                idempotencyKey, request.fromAccountId(), request.toAccountId(), request.amount());

        //barreira 1: requisição duplicada ainda processando
        if (!idempotencyService.tryRegister(idempotencyKey)) {
            if (idempotencyService.isProcessing(idempotencyKey)) {
                return ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(Map.of(
                                "message", "Transferência já está em processamento.",
                                "idempotencyKey", idempotencyKey
                        ));
            }

            //barreira 2: requisição duplicada já finalizada — devolve resposta original
            return idempotencyService.getResponse(idempotencyKey)
                    .map(saved -> ResponseEntity.ok()
                            .body(Map.of(
                                    "message", "Requisição duplicada. Retornando resposta original.",
                                    "idempotencyKey", idempotencyKey,
                                    "originalResponse", saved
                            )))
                    .orElse(ResponseEntity.status(HttpStatus.ACCEPTED).build());
        }
        //inicia a saga
        try {
            SagaExecution saga = sagaOrchestrator.start(
                    idempotencyKey,
                    UUID.fromString(request.fromAccountId()),
                    UUID.fromString(request.toAccountId()),
                    request.amount(),
                    request.currency()
            );
            //salva a resposta no Redis para futuras duplicatas
            idempotencyService.saveResponse(idempotencyKey,
                    "{\"sagaId\":\"" + saga.getId() + "\",\"status\":\"" + saga.getStatus() + "\"}");
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                            "message", "Transferência iniciada com sucesso.",
                            "sagaId", saga.getId(),
                            "status", saga.getStatus(),
                            "idempotencyKey", idempotencyKey
                    ));
        } catch (SagaUnavailableException e) {
            idempotencyService.saveResponse(idempotencyKey,
                    "{\"error\":\"" + e.getMessage() + "\"}");

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}