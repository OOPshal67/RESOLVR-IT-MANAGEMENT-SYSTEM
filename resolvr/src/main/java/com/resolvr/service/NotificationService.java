package com.resolvr.service;

import com.resolvr.entity.Notification;
import com.resolvr.entity.User;

import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsForUser(User user);
    long getUnreadCount(User user);
    void markAsRead(Long notificationId, User currentUser);
    void markAllAsRead(User currentUser);
}
