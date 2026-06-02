package com.banco.digital.dto;

import com.banco.digital.entity.Transfer;
import com.banco.digital.entity.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementResponse(
        Long transferId,
        String type,
        Long counterpartyAccountId,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt
) {

    public static MovementResponse from(Transfer transfer, Long accountId) {
        boolean isOutgoing = transfer.getSourceAccountId().equals(accountId);
        return new MovementResponse(
                transfer.getId(),
                isOutgoing ? "DEBIT" : "CREDIT",
                isOutgoing ? transfer.getDestinationAccountId() : transfer.getSourceAccountId(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
