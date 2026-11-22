package com.resolvr.repository;

import com.resolvr.entity.Incident;
import com.resolvr.entity.IncidentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncidentFeedbackRepository extends JpaRepository<IncidentFeedback, Long> {
    Optional<IncidentFeedback> findByIncident(Incident incident);
}
