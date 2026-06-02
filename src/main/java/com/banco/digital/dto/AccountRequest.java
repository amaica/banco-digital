package com.banco.digital.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank(message = "Informe o nome")
        String name,

        @NotNull(message = "Saldo inicial obrigatorio")
        @DecimalMin(value = "0.00", message = "Saldo nao pode ser negativo")
        BigDecimal initialBalance
) {
}
