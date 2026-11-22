package com.resolvr.state;

import com.resolvr.entity.Incident;
import com.resolvr.enums.IncidentStatus;

import java.util.Set;

public interface IncidentState {
    IncidentStatus getStatus();
    Set<IncidentStatus> getAllowedTransitions();
    void onEnterState(Incident incident);
}
