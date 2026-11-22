package com.resolvr.repository;

import com.resolvr.entity.TechnicianSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianSkillRepository extends JpaRepository<TechnicianSkill, Long> {
    Optional<TechnicianSkill> findBySkillCode(String skillCode);
    List<TechnicianSkill> findByCategoryCode(String categoryCode);
}
