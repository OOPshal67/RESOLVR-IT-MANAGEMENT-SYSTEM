package com.resolvr.repository;

import com.resolvr.entity.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {
    Optional<IncidentCategory> findByCode(String code);
    List<IncidentCategory> findByActiveTrueOrderByNameAsc();
}
