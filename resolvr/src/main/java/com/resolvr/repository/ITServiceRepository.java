package com.resolvr.repository;

import com.resolvr.entity.ITService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITServiceRepository extends JpaRepository<ITService, Long> {
    Optional<ITService> findByServiceCode(String serviceCode);
    List<ITService> findByActiveTrueOrderByNameAsc();
}
