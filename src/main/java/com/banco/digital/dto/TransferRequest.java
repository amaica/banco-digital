package com.banco.digital.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "Informa a conta de origem")
        Long sourceAccountId,

        @NotNull(message = "Informa a conta de destino")
        Long destinationAccountId,

        @NotNull(message = "Informa o valor")
        @DecimalMin(value = "0.01", message = "Valor tem que ser maior que zero")
        BigDecimal amount
) {
}
