package com.banco.digital.event;

import java.math.BigDecimal;

public record TransferCompletedEvent(
        Long transferId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String sourceName,
        String destinationName
) {
}
