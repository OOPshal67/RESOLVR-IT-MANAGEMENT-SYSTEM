package com.resolvr.repository;

import com.resolvr.entity.Incident;
import com.resolvr.entity.IncidentRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRelationshipRepository extends JpaRepository<IncidentRelationship, Long> {
    List<IncidentRelationship> findBySourceIncidentOrTargetIncident(Incident source, Incident target);
    List<IncidentRelationship> findTop20ByOrderBySimilarityScoreDesc();
}
