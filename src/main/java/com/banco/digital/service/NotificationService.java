package com.banco.digital.service;

import com.banco.digital.dto.NotificationResponse;
import com.banco.digital.entity.Notification;
import com.banco.digital.entity.NotificationStatus;
import com.banco.digital.event.TransferCompletedEvent;
import com.banco.digital.repository.NotificationRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyTransferCompleted(TransferCompletedEvent event) {
        sendNotification(
                event.sourceAccountId(),
                event.transferId(),
                String.format("Transferencia enviada: R$ %.2f para %s",
                        event.amount(), event.destinationName())
        );

        sendNotification(
                event.destinationAccountId(),
                event.transferId(),
                String.format("Transferencia recebida: R$ %.2f de %s",
                        event.amount(), event.sourceName())
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listByAccount(Long accountId) {
        return notificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    private void sendNotification(Long accountId, Long transferId, String message) {
        try {
            Notification notification = notificationRepository.save(
                    new Notification(accountId, transferId, message, NotificationStatus.SENT)
            );
            log.info("notificacao ok - conta {} msg {}", accountId, message);
        } catch (Exception ex) {
            log.warn("falha na notificacao conta {}: {}", accountId, ex.getMessage());
            notificationRepository.save(
                    new Notification(accountId, transferId, message, NotificationStatus.FAILED)
            );
        }
    }
}
