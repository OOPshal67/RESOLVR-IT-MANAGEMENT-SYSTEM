package com.resolvr.repository;

import com.resolvr.entity.Incident;
import com.resolvr.entity.SLAViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SLAViolationRepository extends JpaRepository<SLAViolation, Long> {
    List<SLAViolation> findByIncidentOrderByDetectedAtDesc(Incident incident);
    List<SLAViolation> findTop25ByOrderByDetectedAtDesc();
    boolean existsByIncidentAndViolationType(Incident incident, String violationType);
}
