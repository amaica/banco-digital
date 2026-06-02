package com.banco.digital.dto;

import com.banco.digital.entity.Notification;
import com.banco.digital.entity.NotificationStatus;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long accountId,
        Long transferId,
        String message,
        NotificationStatus status,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getAccountId(),
                notification.getTransferId(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
