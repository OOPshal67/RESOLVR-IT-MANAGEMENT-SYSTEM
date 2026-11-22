package com.resolvr;

import com.resolvr.dto.AssignmentRecommendation;
import com.resolvr.dto.PriorityResult;
import com.resolvr.entity.ITService;
import com.resolvr.entity.Incident;
import com.resolvr.entity.IncidentCategory;
import com.resolvr.entity.Technician;
import com.resolvr.entity.TechnicianSkill;
import com.resolvr.enums.BusinessCriticality;
import com.resolvr.enums.ImpactLevel;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.enums.UrgencyLevel;
import com.resolvr.strategy.LoadBasedAssignmentStrategy;
import com.resolvr.strategy.RoundRobinAssignmentStrategy;
import com.resolvr.strategy.SkillBasedAssignmentStrategy;
import com.resolvr.strategy.StrictMatrixPriorityStrategy;
import com.resolvr.strategy.WeightedPriorityCalculationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests 7, 8, 9, 10, 11: Priority Calculation, Priority Boundaries, Skill-Based Assignment,
 * Load-Based Assignment, Round-Robin Assignment, and Unavailable/Overloaded Technician Handling.
 */
class PriorityAndStrategyTest {

    private WeightedPriorityCalculationStrategy weightedStrategy;
    private StrictMatrixPriorityStrategy matrixStrategy;
    private SkillBasedAssignmentStrategy skillStrategy;
    private LoadBasedAssignmentStrategy loadStrategy;
    private RoundRobinAssignmentStrategy roundRobinStrategy;

    private IncidentCategory appCategory;
    private ITService paymentService;

    @BeforeEach
    void setUp() {
        weightedStrategy = new WeightedPriorityCalculationStrategy();
        matrixStrategy = new StrictMatrixPriorityStrategy();
        skillStrategy = new SkillBasedAssignmentStrategy();
        loadStrategy = new LoadBasedAssignmentStrategy();
        roundRobinStrategy = new RoundRobinAssignmentStrategy();

        appCategory = new IncidentCategory("APPLICATION", "Business Applications", "Apps", 1.25);
        paymentService = new ITService("SRV-PAY", "Payment Gateway", "Payment", BusinessCriticality.MISSION_CRITICAL, "FinTech", "OPERATIONAL");
    }

    @Test
    @DisplayName("TC-07: Automatic Priority Calculation produces P1 Critical for Mission-Critical Outage")
    void testPriorityCalculationP1Critical() {
        PriorityResult result = weightedStrategy.calculatePriority(
                ImpactLevel.CRITICAL, UrgencyLevel.CRITICAL, BusinessCriticality.MISSION_CRITICAL,
                350, appCategory, paymentService
        );
        assertEquals(PriorityLevel.P1, result.getPriorityLevel());
        assertEquals(88.0, result.getCalculatedScore(), 0.01);
        assertTrue(result.getReasoning().contains("P1 - Critical"));
    }

    @Test
    @DisplayName("TC-08: Priority Boundary Conditions accurately classify P1, P2, P3, and P4")
    void testPriorityBoundaryConditions() {
        IncidentCategory stdCategory = new IncidentCategory("HARDWARE", "Hardware", "HW", 1.0);
        ITService stdService = new ITService("SRV-STD", "Standard", "Desc", BusinessCriticality.MEDIUM, "IT", "OPERATIONAL");

        // P1 >= 36.0 -> 3 * 3 * 4 * 1.0 = 36.0
        PriorityResult p1 = weightedStrategy.calculatePriority(
                ImpactLevel.HIGH, UrgencyLevel.HIGH, BusinessCriticality.MISSION_CRITICAL, 1, stdCategory, stdService);
        assertEquals(PriorityLevel.P1, p1.getPriorityLevel());

        // P2 >= 18.0 -> 3 * 3 * 2 * 1.0 = 18.0
        PriorityResult p2 = weightedStrategy.calculatePriority(
                ImpactLevel.HIGH, UrgencyLevel.HIGH, BusinessCriticality.MEDIUM, 1, stdCategory, stdService);
        assertEquals(PriorityLevel.P2, p2.getPriorityLevel());

        // P3 >= 7.0 -> 2 * 2 * 2 * 1.0 = 8.0
        PriorityResult p3 = weightedStrategy.calculatePriority(
                ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, BusinessCriticality.MEDIUM, 1, stdCategory, stdService);
        assertEquals(PriorityLevel.P3, p3.getPriorityLevel());

        // P4 < 7.0 -> 1 * 1 * 2 * 1.0 = 2.0
        PriorityResult p4 = weightedStrategy.calculatePriority(
                ImpactLevel.LOW, UrgencyLevel.LOW, BusinessCriticality.MEDIUM, 1, stdCategory, stdService);
        assertEquals(PriorityLevel.P4, p4.getPriorityLevel());
    }

    @Test
    @DisplayName("TC-09: Skill-Based Assignment Strategy selects technician with matching category skill")
    void testSkillBasedTechnicianAssignment() {
        Technician techNoSkill = new Technician("tech1", "t1@resolvr.io", "hash", "Tech One",
                "IT", "111", "TECH201", "HARDWARE", 5, 2, false);
        techNoSkill.setId(1L);

        Technician techMatchingSkill = new Technician("tech2", "t2@resolvr.io", "hash", "Tech Two",
                "IT", "222", "TECH202", "APPLICATION", 5, 5, true);
        techMatchingSkill.setId(2L);
        techMatchingSkill.setSkills(Set.of(new TechnicianSkill("SKILL_APPLICATION", "App Support", "APPLICATION", "Desc")));

        Incident incident = new Incident();
        incident.setCategory(appCategory);
        incident.setService(paymentService);

        Optional<AssignmentRecommendation> rec = skillStrategy.recommendTechnician(
                incident, List.of(techNoSkill, techMatchingSkill));

        assertTrue(rec.isPresent());
        assertEquals("Tech Two", rec.get().getTechnician().getFullName());
        assertTrue(rec.get().getReason().contains("Verified APPLICATION skill match"));
    }

    @Test
    @DisplayName("TC-10: Load-Based Assignment Strategy selects available technician with lowest workload")
    void testLoadBasedTechnicianAssignment() {
        Technician busyTech = new Technician("busy", "b@resolvr.io", "hash", "Busy Tech",
                "IT", "111", "TECH201", "NETWORK", 5, 5, false);
        busyTech.setId(1L);
        busyTech.setCurrentWorkload(4);

        Technician lightTech = new Technician("light", "l@resolvr.io", "hash", "Light Tech",
                "IT", "222", "TECH202", "NETWORK", 5, 3, false);
        lightTech.setId(2L);
        lightTech.setCurrentWorkload(1);

        Incident incident = new Incident();
        Optional<AssignmentRecommendation> rec = loadStrategy.recommendTechnician(
                incident, List.of(busyTech, lightTech));

        assertTrue(rec.isPresent());
        assertEquals("Light Tech", rec.get().getTechnician().getFullName());
    }

    @Test
    @DisplayName("TC-11: Unavailable or max-workload technicians are never recommended")
    void testUnavailableAndMaxWorkloadTechnicianHandling() {
        Technician unavailableTech = new Technician("unavail", "u@resolvr.io", "hash", "Unavailable Tech",
                "IT", "111", "TECH201", "APPLICATION", 5, 5, false);
        unavailableTech.setAvailable(false);

        Technician overloadedTech = new Technician("full", "f@resolvr.io", "hash", "Overloaded Tech",
                "IT", "222", "TECH202", "APPLICATION", 3, 5, false);
        overloadedTech.setCurrentWorkload(3);

        Incident incident = new Incident();
        incident.setCategory(appCategory);

        Optional<AssignmentRecommendation> skillRec = skillStrategy.recommendTechnician(
                incident, List.of(unavailableTech, overloadedTech));
        Optional<AssignmentRecommendation> loadRec = loadStrategy.recommendTechnician(
                incident, List.of(unavailableTech, overloadedTech));
        Optional<AssignmentRecommendation> rrRec = roundRobinStrategy.recommendTechnician(
                incident, List.of(unavailableTech, overloadedTech));

        assertTrue(skillRec.isEmpty());
        assertTrue(loadRec.isEmpty());
        assertTrue(rrRec.isEmpty());
        assertNotNull(matrixStrategy.calculatePriority(ImpactLevel.HIGH, UrgencyLevel.HIGH, BusinessCriticality.HIGH, 50, appCategory, paymentService));
    }
}
