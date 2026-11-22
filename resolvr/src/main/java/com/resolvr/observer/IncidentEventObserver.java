package com.resolvr.observer;

public interface IncidentEventObserver {
    void onIncidentEvent(IncidentDomainEvent event);
}
