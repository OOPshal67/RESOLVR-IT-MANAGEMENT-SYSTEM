package com.resolvr.observer;

import com.resolvr.entity.Notification;
import com.resolvr.entity.User;
import com.resolvr.factory.NotificationFactory;
import com.resolvr.repository.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationObserver implements IncidentEventObserver {

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;

    public InAppNotificationObserver(NotificationRepository notificationRepository,
                                     NotificationFactory notificationFactory) {
        this.notificationRepository = notificationRepository;
        this.notificationFactory = notificationFactory;
    }

    @Override
    public void onIncidentEvent(IncidentDomainEvent event) {
        if (event.getNotificationType() == null || event.getNotificationRecipients().isEmpty()) {
            return;
        }
        String key = event.getIncident() != null ? event.getIncident().getIncidentKey() : "SYSTEM";
        String title = "[" + key + "] " + event.getActionType().replace('_', ' ');

        for (User recipient : event.getNotificationRecipients()) {
            if (recipient != null) {
                Notification notification = notificationFactory.createInAppNotification(
                    recipient,
                    event.getIncident(),
                    event.getNotificationType(),
                    title,
                    event.getDescription()
                );
                notificationRepository.save(notification);
            }
        }
    }
}
