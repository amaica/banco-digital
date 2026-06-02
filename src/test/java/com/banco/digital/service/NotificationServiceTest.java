package com.banco.digital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banco.digital.dto.NotificationResponse;
import com.banco.digital.entity.Notification;
import com.banco.digital.entity.NotificationStatus;
import com.banco.digital.event.TransferCompletedEvent;
import com.banco.digital.repository.NotificationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldListNotificationsByAccount() {
        Notification notification = new Notification(1L, 10L, "Transferencia recebida", NotificationStatus.SENT);
        when(notificationRepository.findByAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.listByAccount(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).message()).contains("Transferencia recebida");
    }

    @Test
    void shouldPersistNotificationsOnTransferCompleted() {
        TransferCompletedEvent event = new TransferCompletedEvent(
                1L, 1L, 2L, new BigDecimal("100.00"), "Alice", "Bruno");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyTransferCompleted(event);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }
}
