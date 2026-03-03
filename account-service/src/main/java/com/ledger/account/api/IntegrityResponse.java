package com.ledger.account.api;

public record IntegrityResponse(
    long totalRegistros,
    long registrosCorrompidos,
    String status
) {}