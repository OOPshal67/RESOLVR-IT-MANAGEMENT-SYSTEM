package com.resolvr.service;

import com.resolvr.entity.Notification;
import com.resolvr.entity.User;
import com.resolvr.exception.UnauthorizedOperationException;
import com.resolvr.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DatabaseNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;

    public DatabaseNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndReadStatusFalse(user);
    }

    @Override
    public void markAsRead(Long notificationId, User currentUser) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!n.getRecipient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedOperationException("Cannot modify another user's notification.");
        }
        n.markAsRead();
        notificationRepository.save(n);
    }

    @Override
    public void markAllAsRead(User currentUser) {
        notificationRepository.markUnreadForRecipient(currentUser);
    }
}
