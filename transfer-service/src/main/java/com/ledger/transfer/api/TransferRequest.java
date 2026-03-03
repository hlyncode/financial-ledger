package com.ledger.transfer.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Conta de origem é obrigatória")
        String fromAccountId,

        @NotBlank(message = "Conta de destino é obrigatória")
        String toAccountId,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor mínimo é 0.01")
        BigDecimal amount,

        @NotBlank(message = "Moeda é obrigatória")
        String currency
) {}