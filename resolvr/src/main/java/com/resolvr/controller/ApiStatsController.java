package com.resolvr.controller;

import com.resolvr.dto.DashboardStatsDto;
import com.resolvr.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "RESOLVR Real-Time Metrics API", description = "Endpoints for live dashboard polling and SLA statistics")
@PreAuthorize("hasRole('ADMIN')")
public class ApiStatsController {

    private final AnalyticsService analyticsService;

    public ApiStatsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Retrieve live system-wide incident and SLA metrics")
    public ResponseEntity<Map<String, Object>> getLiveDashboardStats() {
        DashboardStatsDto stats = analyticsService.computeDashboardStats();
        return ResponseEntity.ok(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "stats", stats,
            "categories", analyticsService.getCategoryBreakdown(),
            "services", analyticsService.getServiceBreakdown(),
            "workload", analyticsService.getTechnicianWorkloadMap()
        ));
    }
}
