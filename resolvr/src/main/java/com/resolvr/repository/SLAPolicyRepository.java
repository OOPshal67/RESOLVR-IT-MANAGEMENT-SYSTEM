package com.resolvr.repository;

import com.resolvr.entity.SLAPolicy;
import com.resolvr.enums.PriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SLAPolicyRepository extends JpaRepository<SLAPolicy, Long> {
    Optional<SLAPolicy> findByPriorityLevel(PriorityLevel priorityLevel);
}
