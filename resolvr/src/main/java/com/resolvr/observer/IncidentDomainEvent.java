package com.resolvr.observer;

import com.resolvr.entity.Incident;
import com.resolvr.entity.User;
import com.resolvr.enums.NotificationType;

import java.util.ArrayList;
import java.util.List;

public class IncidentDomainEvent {
    private final Incident incident;
    private final String actorUsername;
    private final String actorRole;
    private final String actionType;
    private final String previousState;
    private final String newState;
    private final String description;
    private final String metadata;
    private final NotificationType notificationType;
    private final List<User> notificationRecipients = new ArrayList<>();

    public IncidentDomainEvent(Incident incident,
                               String actorUsername,
                               String actorRole,
                               String actionType,
                               String previousState,
                               String newState,
                               String description,
                               String metadata,
                               NotificationType notificationType,
                               List<User> recipients) {
        this.incident = incident;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
        this.metadata = metadata;
        this.notificationType = notificationType;
        if (recipients != null) {
            this.notificationRecipients.addAll(recipients);
        }
    }

    public Incident getIncident() { return incident; }
    public String getActorUsername() { return actorUsername; }
    public String getActorRole() { return actorRole; }
    public String getActionType() { return actionType; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    public NotificationType getNotificationType() { return notificationType; }
    public List<User> getNotificationRecipients() { return notificationRecipients; }
}
