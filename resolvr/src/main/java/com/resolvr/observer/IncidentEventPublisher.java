package com.resolvr.observer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentEventPublisher {

    private final List<IncidentEventObserver> observers;

    public IncidentEventPublisher(List<IncidentEventObserver> observers) {
        this.observers = observers;
    }

    public void publish(IncidentDomainEvent event) {
        for (IncidentEventObserver observer : observers) {
            observer.onIncidentEvent(event);
        }
    }
}
