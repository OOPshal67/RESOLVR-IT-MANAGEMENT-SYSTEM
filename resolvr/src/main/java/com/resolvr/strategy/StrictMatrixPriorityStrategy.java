package com.resolvr.strategy;

import com.resolvr.dto.PriorityResult;
import com.resolvr.entity.ITService;
import com.resolvr.entity.IncidentCategory;
import com.resolvr.enums.BusinessCriticality;
import com.resolvr.enums.ImpactLevel;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.enums.UrgencyLevel;
import org.springframework.stereotype.Component;

/**
 * Alternative ITIL Matrix-based priority strategy focusing on Impact + Urgency + Mission-Critical floor.
 */
@Component
public class StrictMatrixPriorityStrategy implements PriorityCalculationStrategy {

    @Override
    public String getStrategyCode() {
        return "ITIL_MATRIX";
    }

    @Override
    public String getStrategyDisplayName() {
        return "ITIL Impact-Urgency Matrix Strategy";
    }

    @Override
    public PriorityResult calculatePriority(ImpactLevel impact,
                                            UrgencyLevel urgency,
                                            BusinessCriticality criticality,
                                            int affectedUsers,
                                            IncidentCategory category,
                                            ITService service) {
        int imp = impact != null ? impact.getWeight() : 2;
        int urg = urgency != null ? urgency.getWeight() : 2;
        int crit = criticality != null ? criticality.getWeight() : 2;

        int matrixSum = imp + urg + (crit == 4 ? 1 : 0) + (affectedUsers >= 100 ? 1 : 0);
        PriorityLevel level;
        if (matrixSum >= 8) {
            level = PriorityLevel.P1;
        } else if (matrixSum >= 6) {
            level = PriorityLevel.P2;
        } else if (matrixSum >= 4) {
            level = PriorityLevel.P3;
        } else {
            level = PriorityLevel.P4;
        }

        String reasoning = String.format(
            "ITIL Matrix Index %d [Impact=%d + Urgency=%d + CritBoost=%d + MassUserBoost=%d] => %s",
            matrixSum, imp, urg, (crit == 4 ? 1 : 0), (affectedUsers >= 100 ? 1 : 0), level.getLabel()
        );
        return new PriorityResult(level, matrixSum * 5.0, reasoning);
    }
}
