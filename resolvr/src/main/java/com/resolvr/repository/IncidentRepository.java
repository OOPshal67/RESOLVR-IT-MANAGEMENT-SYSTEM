package com.resolvr.repository;

import com.resolvr.entity.Employee;
import com.resolvr.entity.Incident;
import com.resolvr.entity.Technician;
import com.resolvr.enums.EscalationLevel;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.PriorityLevel;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    @Query(value = "select nextval('incident_number')", nativeQuery = true)
    long nextIncidentNumber();

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "slaPolicy", "resolution", "feedback"})
    List<Incident> findAll();
    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "slaPolicy", "resolution", "feedback"})
    Page<Incident> findAll(Specification<Incident> spec, Pageable pageable);
    Optional<Incident> findByIncidentKey(String incidentKey);
    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "slaPolicy", "resolution", "feedback"})
    List<Incident> findByReporterOrderByCreatedAtDesc(Employee reporter);
    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "slaPolicy", "resolution", "feedback"})
    List<Incident> findByAssignedTechnicianOrderByPriorityAscCreatedAtDesc(Technician technician);
    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "slaPolicy", "resolution", "feedback"})
    List<Incident> findByStatusNotIn(Collection<IncidentStatus> statuses);
    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "slaPolicy", "resolution", "feedback"})
    List<Incident> findTop10ByOrderByCreatedAtDesc();
    long countByStatusIn(Collection<IncidentStatus> statuses);
    interface MetricRow {
        IncidentStatus getStatus();
        PriorityLevel getPriority();
        EscalationLevel getEscalationLevel();
        boolean getResponseSlaViolated();
        boolean getResolutionSlaViolated();
        Long getAssignee();
        LocalDateTime getResolvedAt();
        LocalDateTime getCreatedAt();
    }

    @Query("""
            select i.status as status, i.priority as priority, i.escalationLevel as escalationLevel,
                   i.responseSlaViolated as responseSlaViolated, i.resolutionSlaViolated as resolutionSlaViolated,
                   i.assignedTechnician.id as assignee, i.createdAt as createdAt, i.resolvedAt as resolvedAt
            from Incident i
            """)
    List<MetricRow> readMetrics();

    long countByPriority(PriorityLevel priority);
    long countByResponseSlaViolatedTrueOrResolutionSlaViolatedTrue();
    long countByAssignedTechnicianIsNull();

    @Query("SELECT i.category.name, COUNT(i) FROM Incident i GROUP BY i.category.name ORDER BY COUNT(i) DESC")
    List<Object[]> countIncidentsByCategory();

    @Query("SELECT i.service.name, COUNT(i) FROM Incident i GROUP BY i.service.name ORDER BY COUNT(i) DESC")
    List<Object[]> countIncidentsByService();

    @Query("SELECT i.priority, COUNT(i) FROM Incident i GROUP BY i.priority ORDER BY i.priority ASC")
    List<Object[]> countIncidentsByPriority();
}
