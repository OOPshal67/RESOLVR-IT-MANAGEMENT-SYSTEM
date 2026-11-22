package com.resolvr.repository;

import com.resolvr.entity.Technician;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    @Query(value = "select nextval('technician_number')", nativeQuery = true)
    long nextTechnicianNumber();

    @EntityGraph(attributePaths = "skills")
    List<Technician> findAll();
    Optional<Technician> findByUsername(String username);
    Optional<Technician> findByTechnicianCode(String technicianCode);
    @EntityGraph(attributePaths = "skills")
    List<Technician> findByAvailableTrueAndActiveTrue();
    List<Technician> findByTeamLeadTrueAndActiveTrue();
}
