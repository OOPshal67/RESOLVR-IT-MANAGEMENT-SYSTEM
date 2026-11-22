package com.resolvr.repository;

import com.resolvr.entity.AuditLog;
import com.resolvr.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByIncidentOrderByActionTimestampDesc(Incident incident);
    List<AuditLog> findTop100ByOrderByActionTimestampDesc();
    List<AuditLog> findByActionTypeOrderByActionTimestampDesc(String actionType);
}
