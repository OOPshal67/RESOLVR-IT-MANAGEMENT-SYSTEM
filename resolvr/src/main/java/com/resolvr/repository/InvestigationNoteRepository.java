package com.resolvr.repository;

import com.resolvr.entity.Incident;
import com.resolvr.entity.InvestigationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestigationNoteRepository extends JpaRepository<InvestigationNote, Long> {
    List<InvestigationNote> findByIncidentOrderByCreatedAtAsc(Incident incident);
}
