package com.resolvr.service;

import com.resolvr.dto.DashboardStatsDto;
import com.resolvr.entity.Incident;
import com.resolvr.entity.SLAViolation;
import com.resolvr.entity.Technician;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.repository.IncidentRepository;
import com.resolvr.repository.SLAViolationRepository;
import com.resolvr.repository.TechnicianRepository;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final SLAViolationRepository slaViolationRepository;

    public AnalyticsService(IncidentRepository incidentRepository,
                            TechnicianRepository technicianRepository,
                            SLAViolationRepository slaViolationRepository) {
        this.incidentRepository = incidentRepository;
        this.technicianRepository = technicianRepository;
        this.slaViolationRepository = slaViolationRepository;
    }

    public DashboardStatsDto computeDashboardStats() {
        var all = incidentRepository.readMetrics();
        DashboardStatsDto stats = new DashboardStatsDto();

        long total = all.size();
        long open = all.stream().filter(i -> i.getStatus().isActive()).count();
        long p1 = all.stream().filter(i -> i.getPriority() == PriorityLevel.P1).count();
        long p2 = all.stream().filter(i -> i.getPriority() == PriorityLevel.P2).count();
        long p3 = all.stream().filter(i -> i.getPriority() == PriorityLevel.P3).count();
        long p4 = all.stream().filter(i -> i.getPriority() == PriorityLevel.P4).count();
        long resolved = all.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.VERIFIED).count();
        long closed = all.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).count();
        long violated = all.stream().filter(i -> i.getResponseSlaViolated() || i.getResolutionSlaViolated()).count();
        long unassigned = all.stream().filter(i -> i.getAssignee() == null && i.getStatus().isActive()).count();
        long escalated = all.stream().filter(i -> i.getStatus() == IncidentStatus.ESCALATED || i.getEscalationLevel().getLevel() >= 2).count();

        double compliance = total > 0 ? Math.round(((total - violated) * 1000.0) / total) / 10.0 : 100.0;

        OptionalDouble avgResMins = all.stream()
                .filter(i -> i.getResolvedAt() != null && i.getCreatedAt() != null)
                .mapToLong(i -> Math.max(1, Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes()))
                .average();

        stats.setTotalIncidents(total);
        stats.setOpenIncidents(open);
        stats.setCriticalIncidents(p1);
        stats.setHighPriorityIncidents(p2);
        stats.setMediumPriorityIncidents(p3);
        stats.setLowPriorityIncidents(p4);
        stats.setResolvedIncidents(resolved);
        stats.setClosedIncidents(closed);
        stats.setSlaViolations(violated);
        stats.setAwaitingAssignment(unassigned);
        stats.setEscalatedIncidents(escalated);
        stats.setSlaComplianceRate(compliance);
        stats.setAverageResolutionMinutes(Math.round(avgResMins.orElse(0.0) * 10.0) / 10.0);

        return stats;
    }

    public Map<String, Long> getCategoryBreakdown() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : incidentRepository.countIncidentsByCategory()) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    public Map<String, Long> getServiceBreakdown() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : incidentRepository.countIncidentsByService()) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    public Map<String, Integer> getTechnicianWorkloadMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Technician tech : technicianRepository.findAll()) {
            map.put(tech.getFullName(), tech.getCurrentWorkload());
        }
        return map;
    }

    public List<SLAViolation> getRecentSLAViolations() {
        return slaViolationRepository.findTop25ByOrderByDetectedAtDesc();
    }

    public String generateCsvExport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Incident_Key,Title,Category,IT_Service,Priority,Status,Reporter,Assigned_Technician,Created_At,Response_Deadline,Resolution_Deadline,SLA_Violated\n");
        for (Incident inc : incidentRepository.findAll()) {
            sb.append(escapeCsv(inc.getIncidentKey())).append(",")
              .append(escapeCsv(inc.getTitle())).append(",")
              .append(escapeCsv(inc.getCategory() != null ? inc.getCategory().getName() : "")).append(",")
              .append(escapeCsv(inc.getService() != null ? inc.getService().getName() : "")).append(",")
              .append(escapeCsv(inc.getPriority().name())).append(",")
              .append(escapeCsv(inc.getStatus().name())).append(",")
              .append(escapeCsv(inc.getReporter() != null ? inc.getReporter().getFullName() : "")).append(",")
              .append(escapeCsv(inc.getAssignedTechnician() != null ? inc.getAssignedTechnician().getFullName() : "Unassigned")).append(",")
              .append(escapeCsv(String.valueOf(inc.getCreatedAt()))).append(",")
              .append(escapeCsv(String.valueOf(inc.getResponseDeadline()))).append(",")
              .append(escapeCsv(String.valueOf(inc.getResolutionDeadline()))).append(",")
              .append(inc.isSlaViolated() ? "YES" : "NO")
              .append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (!val.stripLeading().isEmpty() && "=+-@".indexOf(val.stripLeading().charAt(0)) >= 0) val = "'" + val;
        String cleaned = val.replace("\"", "\"\"");
        return "\"" + cleaned + "\"";
    }
}
