package com.resolvr.repository;

import com.resolvr.entity.Assignment;
import com.resolvr.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByIncidentOrderByAssignedAtDesc(Incident incident);
}
