package com.ledger.transfer.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;

    //chave ficar guardada por 24h - tempo suficiente para qualquer retry
    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:";

    //tenta registrar a chave no Redis: retorna true se a chave aparece pela 1° vez e false se ela já existia.
    public boolean tryRegister(String idempotencyKey) {
        String redisKey = PREFIX + idempotencyKey;

        //setIfAbsent é atômico - não há race condition
        //dois threads simultâneos com a msm chave - apenas um retorna true
        Boolean registrado = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", TTL);
        boolean isPrimeira = Boolean.TRUE.equals(registrado);
        if (!isPrimeira) {
            log.warn("Requisição duplicada detectada. idempotencyKey={}", idempotencyKey);
        }
        return isPrimeira;
    }
    //salva a reposta final associada à chave.
    public void saveResponse(String idempotencyKey, String respondeJson) {
        String redisKey = PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(redisKey, respondeJson, TTL);
        log.debug("Resposta salva no Redis. idempotencyKey={}", idempotencyKey);
    }
    //busca a resposta já salva para uma chave duplicada
    public Optional<String> getResponse(String idempotencyKey) {
        String redisKey = PREFIX + idempotencyKey;
        String value = redisTemplate.opsForValue().get(redisKey);
        if ("PROCESSING".equals(value)) {
            return Optional.empty();
        }
        return Optional.ofNullable(value);
    }
    public boolean isProcessing(String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(PREFIX + idempotencyKey);
        return "PROCESSING".equals(value);
    }
}