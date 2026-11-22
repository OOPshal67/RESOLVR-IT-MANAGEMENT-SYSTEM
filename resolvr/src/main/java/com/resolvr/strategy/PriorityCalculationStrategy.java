package com.resolvr.strategy;

import com.resolvr.dto.PriorityResult;
import com.resolvr.entity.ITService;
import com.resolvr.entity.IncidentCategory;
import com.resolvr.enums.BusinessCriticality;
import com.resolvr.enums.ImpactLevel;
import com.resolvr.enums.UrgencyLevel;

public interface PriorityCalculationStrategy {
    String getStrategyCode();
    String getStrategyDisplayName();
    PriorityResult calculatePriority(ImpactLevel impact,
                                     UrgencyLevel urgency,
                                     BusinessCriticality criticality,
                                     int affectedUsers,
                                     IncidentCategory category,
                                     ITService service);
}
