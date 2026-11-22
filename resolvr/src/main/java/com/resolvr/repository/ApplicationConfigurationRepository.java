package com.resolvr.repository;

import com.resolvr.entity.ApplicationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationConfigurationRepository extends JpaRepository<ApplicationConfiguration, Long> {
    Optional<ApplicationConfiguration> findByConfigKey(String configKey);
}
