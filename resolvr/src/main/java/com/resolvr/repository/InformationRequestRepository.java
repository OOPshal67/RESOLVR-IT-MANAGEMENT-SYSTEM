package com.resolvr.repository;

import com.resolvr.entity.Incident;
import com.resolvr.entity.InformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InformationRequestRepository extends JpaRepository<InformationRequest, Long> {
    List<InformationRequest> findByIncidentOrderByRequestedAtDesc(Incident incident);
}
