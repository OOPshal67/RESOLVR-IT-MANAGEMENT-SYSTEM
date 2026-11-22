package com.resolvr.factory;

import com.resolvr.entity.Incident;
import com.resolvr.entity.Notification;
import com.resolvr.entity.User;
import com.resolvr.enums.NotificationChannel;
import com.resolvr.enums.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Factory Pattern implementation for constructing multi-channel Notifications.
 */
@Component
public class NotificationFactory {

    public Notification createInAppNotification(User recipient,
                                                Incident incident,
                                                NotificationType type,
                                                String title,
                                                String message) {
        return new Notification(recipient, incident, type, NotificationChannel.IN_APP, title, message);
    }

    public Notification createSimulatedEmailNotification(User recipient,
                                                         Incident incident,
                                                         NotificationType type,
                                                         String subject,
                                                         String body) {
        String formattedBody = String.format(
            "[SIMULATED EMAIL TO: %s <%s>] Subject: %s | %s",
            recipient.getFullName(), recipient.getEmail(), subject, body
        );
        return new Notification(recipient, incident, type, NotificationChannel.SIMULATED_EMAIL, subject, formattedBody);
    }
}
