package com.resolvr.repository;

import com.resolvr.entity.EscalationRecord;
import com.resolvr.entity.Incident;
import com.resolvr.enums.EscalationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRecordRepository extends JpaRepository<EscalationRecord, Long> {
    List<EscalationRecord> findByIncidentOrderByEscalatedAtDesc(Incident incident);
    List<EscalationRecord> findTop25ByOrderByEscalatedAtDesc();
    boolean existsByIncidentAndEscalationLevel(Incident incident, EscalationLevel escalationLevel);
}
