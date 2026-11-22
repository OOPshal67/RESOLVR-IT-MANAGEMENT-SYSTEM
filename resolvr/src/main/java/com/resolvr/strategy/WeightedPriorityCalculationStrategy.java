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
 * Default weighted scoring priority strategy:
 * Base Score = Impact (1..4) * Urgency (1..4) * BusinessCriticality (1..4) * CategoryWeight
 * User Adjustment:
 *   >= 500 users -> +12.0
 *   >= 100 users -> +8.0
 *   >= 25 users  -> +4.0
 *   >= 5 users   -> +2.0
 *   < 5 users    -> +0.0
 * Thresholds:
 *   Score >= 36.0 -> P1 (CRITICAL)
 *   Score >= 18.0 -> P2 (HIGH)
 *   Score >= 7.0  -> P3 (MEDIUM)
 *   Score < 7.0   -> P4 (LOW)
 */
@Component
public class WeightedPriorityCalculationStrategy implements PriorityCalculationStrategy {

    @Override
    public String getStrategyCode() {
        return "WEIGHTED_SCORE";
    }

    @Override
    public String getStrategyDisplayName() {
        return "Weighted Multi-Factor Scoring Algorithm";
    }

    @Override
    public PriorityResult calculatePriority(ImpactLevel impact,
                                            UrgencyLevel urgency,
                                            BusinessCriticality criticality,
                                            int affectedUsers,
                                            IncidentCategory category,
                                            ITService service) {
        int impWeight = impact != null ? impact.getWeight() : 2;
        int urgWeight = urgency != null ? urgency.getWeight() : 2;
        int critWeight = criticality != null ? criticality.getWeight()
                : (service != null && service.getBusinessCriticality() != null ? service.getBusinessCriticality().getWeight() : 2);
        double catMultiplier = category != null && category.getDefaultWeight() > 0 ? category.getDefaultWeight() : 1.0;

        double baseScore = impWeight * urgWeight * critWeight * catMultiplier;
        double userBonus = calculateAffectedUserAdjustment(affectedUsers);
        double totalScore = Math.round((baseScore + userBonus) * 100.0) / 100.0;

        PriorityLevel level;
        if (totalScore >= 36.0) {
            level = PriorityLevel.P1;
        } else if (totalScore >= 18.0) {
            level = PriorityLevel.P2;
        } else if (totalScore >= 7.0) {
            level = PriorityLevel.P3;
        } else {
            level = PriorityLevel.P4;
        }

        String reasoning = String.format(
            "Weighted Score %.1f [Impact(%s=%d) × Urgency(%s=%d) × Criticality(%s=%d) × CatWeight(%.2f) + UserAdj(+%.1f for %d users)] => %s",
            totalScore,
            impact != null ? impact.name() : "MEDIUM", impWeight,
            urgency != null ? urgency.name() : "MEDIUM", urgWeight,
            criticality != null ? criticality.name() : "MEDIUM", critWeight,
            catMultiplier, userBonus, Math.max(1, affectedUsers), level.getLabel()
        );

        return new PriorityResult(level, totalScore, reasoning);
    }

    private double calculateAffectedUserAdjustment(int affectedUsers) {
        if (affectedUsers >= 500) return 12.0;
        if (affectedUsers >= 100) return 8.0;
        if (affectedUsers >= 25) return 4.0;
        if (affectedUsers >= 5) return 2.0;
        return 0.0;
    }
}
