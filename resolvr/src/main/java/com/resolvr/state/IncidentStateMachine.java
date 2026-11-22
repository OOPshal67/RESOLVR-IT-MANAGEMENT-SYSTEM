package com.resolvr.state;

import com.resolvr.entity.Incident;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the State Design Pattern for strict enforcement of the RESOLVR incident lifecycle:
 * NEW -> CLASSIFIED -> ASSIGNED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED -> VERIFIED -> CLOSED
 * plus WAITING_FOR_USER, ESCALATED, REOPENED, and CANCELLED state transitions.
 */
@Component
public class IncidentStateMachine {

    private final Map<IncidentStatus, IncidentState> stateRegistry = new EnumMap<>(IncidentStatus.class);

    public IncidentStateMachine() {
        register(IncidentStatus.NEW,
            EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED, IncidentStatus.CANCELLED),
            inc -> {});

        register(IncidentStatus.CLASSIFIED,
            EnumSet.of(IncidentStatus.ASSIGNED, IncidentStatus.ESCALATED, IncidentStatus.CANCELLED),
            inc -> {});

        register(IncidentStatus.ASSIGNED,
            EnumSet.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.CLASSIFIED, IncidentStatus.ESCALATED, IncidentStatus.CANCELLED),
            inc -> {
                if (inc.getAssignedAt() == null) inc.setAssignedAt(LocalDateTime.now());
            });

        register(IncidentStatus.ACKNOWLEDGED,
            EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.WAITING_FOR_USER, IncidentStatus.ESCALATED),
            inc -> {
                LocalDateTime now = LocalDateTime.now();
                if (inc.getAcknowledgedAt() == null) inc.setAcknowledgedAt(now);
                if (inc.getFirstResponseAt() == null) inc.setFirstResponseAt(now);
            });

        register(IncidentStatus.IN_PROGRESS,
            EnumSet.of(IncidentStatus.WAITING_FOR_USER, IncidentStatus.RESOLVED, IncidentStatus.ESCALATED, IncidentStatus.ASSIGNED),
            inc -> {
                if (inc.getFirstResponseAt() == null) inc.setFirstResponseAt(LocalDateTime.now());
            });

        register(IncidentStatus.WAITING_FOR_USER,
            EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, IncidentStatus.ESCALATED, IncidentStatus.CANCELLED),
            inc -> {});

        register(IncidentStatus.ESCALATED,
            EnumSet.of(IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED),
            inc -> {});

        register(IncidentStatus.RESOLVED,
            EnumSet.of(IncidentStatus.VERIFIED, IncidentStatus.REOPENED, IncidentStatus.IN_PROGRESS),
            inc -> {
                if (inc.getResolvedAt() == null) inc.setResolvedAt(LocalDateTime.now());
            });

        register(IncidentStatus.VERIFIED,
            EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.REOPENED),
            inc -> {
                if (inc.getVerifiedAt() == null) inc.setVerifiedAt(LocalDateTime.now());
            });

        register(IncidentStatus.REOPENED,
            EnumSet.of(IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, IncidentStatus.ESCALATED),
            inc -> {
                inc.setResolvedAt(null);
                inc.setVerifiedAt(null);
                inc.setClosedAt(null);
            });

        register(IncidentStatus.CLOSED,
            EnumSet.noneOf(IncidentStatus.class),
            inc -> {
                if (inc.getClosedAt() == null) inc.setClosedAt(LocalDateTime.now());
            });

        register(IncidentStatus.CANCELLED,
            EnumSet.noneOf(IncidentStatus.class),
            inc -> {
                if (inc.getClosedAt() == null) inc.setClosedAt(LocalDateTime.now());
            });
    }

    private void register(IncidentStatus status, Set<IncidentStatus> allowed, java.util.function.Consumer<Incident> onEnter) {
        stateRegistry.put(status, new IncidentState() {
            @Override
            public IncidentStatus getStatus() {
                return status;
            }

            @Override
            public Set<IncidentStatus> getAllowedTransitions() {
                return allowed;
            }

            @Override
            public void onEnterState(Incident incident) {
                onEnter.accept(incident);
            }
        });
    }

    public boolean canTransition(IncidentStatus currentStatus, IncidentStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        if (currentStatus == targetStatus) return true;
        IncidentState state = stateRegistry.get(currentStatus);
        return state != null && state.getAllowedTransitions().contains(targetStatus);
    }

    public Set<IncidentStatus> getAllowedNextStatuses(IncidentStatus currentStatus) {
        IncidentState state = stateRegistry.get(currentStatus);
        return state != null ? state.getAllowedTransitions() : Set.of();
    }

    public void transition(Incident incident, IncidentStatus targetStatus) {
        IncidentStatus current = incident.getStatus();
        if (current == targetStatus) {
            return;
        }
        if (!canTransition(current, targetStatus)) {
            throw new InvalidStateTransitionException(String.format(
                "Invalid incident lifecycle transition from %s to %s for incident %s. Allowed transitions from %s: %s",
                current, targetStatus, incident.getIncidentKey(), current, getAllowedNextStatuses(current)
            ));
        }
        if (targetStatus == IncidentStatus.CLOSED && current != IncidentStatus.VERIFIED) {
            throw new InvalidStateTransitionException(
                "An incident cannot be transitioned to CLOSED before resolution is VERIFIED."
            );
        }
        incident.setStatus(targetStatus);
        incident.setUpdatedAt(LocalDateTime.now());
        IncidentState targetStateObj = stateRegistry.get(targetStatus);
        if (targetStateObj != null) {
            targetStateObj.onEnterState(incident);
        }
    }
}
