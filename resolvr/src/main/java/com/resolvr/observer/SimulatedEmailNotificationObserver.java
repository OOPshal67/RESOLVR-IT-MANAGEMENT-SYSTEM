package com.resolvr.observer;

import com.resolvr.entity.Notification;
import com.resolvr.entity.User;
import com.resolvr.factory.NotificationFactory;
import com.resolvr.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedEmailNotificationObserver implements IncidentEventObserver {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailNotificationObserver.class);

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;

    public SimulatedEmailNotificationObserver(NotificationRepository notificationRepository,
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
        String subject = "RESOLVR Alert — " + key + ": " + event.getActionType().replace('_', ' ');

        for (User recipient : event.getNotificationRecipients()) {
            if (recipient != null) {
                Notification emailRecord = notificationFactory.createSimulatedEmailNotification(
                    recipient,
                    event.getIncident(),
                    event.getNotificationType(),
                    subject,
                    event.getDescription()
                );
                notificationRepository.save(emailRecord);
                log.info("Simulated Email Dispatched -> To: {} | Subject: {}", recipient.getEmail(), subject);
            }
        }
    }
}
