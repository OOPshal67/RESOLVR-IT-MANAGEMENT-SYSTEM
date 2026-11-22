package com.resolvr.repository;

import com.resolvr.entity.Incident;
import com.resolvr.entity.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
    Optional<Resolution> findByIncident(Incident incident);
}
