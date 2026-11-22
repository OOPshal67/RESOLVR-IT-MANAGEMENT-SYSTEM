package com.resolvr.controller;

import com.resolvr.dto.AssignmentRecommendation;
import com.resolvr.dto.DashboardStatsDto;
import com.resolvr.dto.IncidentSearchCriteria;
import com.resolvr.entity.Incident;
import com.resolvr.enums.*;
import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IncidentService incidentService;
    private final UserService userService;
    private final CatalogService catalogService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final EscalationService escalationService;

    public AdminController(IncidentService incidentService,
                           UserService userService,
                           CatalogService catalogService,
                           AnalyticsService analyticsService,
                           AuditService auditService,
                           DuplicateDetectionService duplicateDetectionService,
                           EscalationService escalationService) {
        this.incidentService = incidentService;
        this.userService = userService;
        this.catalogService = catalogService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.escalationService = escalationService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        DashboardStatsDto stats = analyticsService.computeDashboardStats();
        Map<String, Long> categoryMap = analyticsService.getCategoryBreakdown();
        Map<String, Long> serviceMap = analyticsService.getServiceBreakdown();
        Map<String, Integer> workloadMap = analyticsService.getTechnicianWorkloadMap();

        model.addAttribute("activePage", "admin-dashboard");
        model.addAttribute("stats", stats);
        model.addAttribute("recentIncidents", incidentService.getRecentIncidents());
        model.addAttribute("technicians", userService.getAllTechnicians());
        model.addAttribute("slaViolations", analyticsService.getRecentSLAViolations());
        model.addAttribute("escalations", escalationService.getRecentEscalations());
        model.addAttribute("duplicateRelationships", duplicateDetectionService.getAllDetectedRelationships());
        model.addAttribute("categoryLabels", categoryMap.keySet());
        model.addAttribute("categoryValues", categoryMap.values());
        model.addAttribute("serviceLabels", serviceMap.keySet());
        model.addAttribute("serviceValues", serviceMap.values());
        model.addAttribute("workloadLabels", workloadMap.keySet());
        model.addAttribute("workloadValues", workloadMap.values());
        return "admin/dashboard";
    }

    @GetMapping("/incidents")
    public String allIncidentsManagement(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @Valid @ModelAttribute("criteria") IncidentSearchCriteria criteria,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) Long selectedId,
                                         Model model) {
        Page<Incident> incidentPage = incidentService.searchIncidents(criteria, userDetails.getUser(), page, size);
        DashboardStatsDto stats = analyticsService.computeDashboardStats();

        Incident selectedIncident = null;
        if (selectedId != null) {
            selectedIncident = incidentService.getIncidentById(selectedId);
        } else if (!incidentPage.getContent().isEmpty()) {
            selectedIncident = incidentPage.getContent().get(0);
        }

        model.addAttribute("activePage", "admin-incidents");
        model.addAttribute("incidentPage", incidentPage);
        model.addAttribute("stats", stats);
        model.addAttribute("selectedIncident", selectedIncident);
        if (selectedIncident != null) {
            Optional<AssignmentRecommendation> rec = incidentService.recommendTechnicianForIncident(selectedIncident);
            rec.ifPresent(r -> model.addAttribute("smartRecommendation", r));
            model.addAttribute("selectedAuditLogs", auditService.getAuditLogsForIncident(selectedIncident));
            model.addAttribute("selectedRelationships", duplicateDetectionService.getRelationshipsForIncident(selectedIncident));
        }
        model.addAttribute("categories", catalogService.getAllCategories());
        model.addAttribute("services", catalogService.getAllServices());
        model.addAttribute("technicians", userService.getAllTechnicians());
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", PriorityLevel.values());
        return "admin/incidents";
    }

    @PostMapping("/incidents/{id}/assign")
    public String assignTechnician(@PathVariable Long id,
                                   @RequestParam Long technicianId,
                                   @RequestParam(required = false) @Size(max = 255) String reason,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        incidentService.assignIncidentToTechnician(
            id, technicianId, userDetails.getUsername(), "ADMIN_ASSIGNMENT",
            reason != null && !reason.isBlank() ? reason : "Assigned by Administrator " + userDetails.getUsername()
        );
        redirectAttributes.addFlashAttribute("successMessage", "Incident assigned to technician successfully.");
        return "redirect:/admin/incidents?selectedId=" + id;
    }

    @PostMapping("/incidents/{id}/escalate")
    public String manualEscalate(@PathVariable Long id,
                                 @RequestParam(required = false) @Size(max = 255) String reason,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        Incident inc = incidentService.getIncidentById(id);
        escalationService.manualEscalate(inc, reason, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage",
                "Incident " + inc.getIncidentKey() + " escalated to " + inc.getEscalationLevel().getDescription());
        return "redirect:/admin/incidents?selectedId=" + id;
    }

    @PostMapping("/incidents/{id}/priority")
    public String recalculatePriority(@PathVariable Long id,
                                      @RequestParam ImpactLevel impact,
                                      @RequestParam UrgencyLevel urgency,
                                      @RequestParam @Min(1) int affectedUsers,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        Incident updated = incidentService.recalculateAndUpdatePriority(
                id, impact, urgency, affectedUsers, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage",
                "Priority recalculated to " + updated.getPriority().getLabel() + " (" + updated.getPriorityReasoning() + ")");
        return "redirect:/admin/incidents?selectedId=" + id;
    }

    @PostMapping("/relationships/{id}/confirm")
    public String confirmDuplicate(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        duplicateDetectionService.confirmDuplicateRelationship(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Duplicate incident relationship confirmed in database.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/users")
    public String userManagementPage(Model model) {
        model.addAttribute("activePage", "admin-users");
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-active")
    public String toggleUserActive(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        userService.toggleUserActiveStatus(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "User account status updated.");
        return "redirect:/admin/users";
    }

    @GetMapping("/technicians")
    public String technicianManagementPage(Model model) {
        model.addAttribute("activePage", "admin-technicians");
        model.addAttribute("technicians", userService.getAllTechnicians());
        model.addAttribute("allSkills", userService.getAllSkills());
        return "admin/technicians";
    }

    @PostMapping("/technicians")
    public String createTechnician(@RequestParam @Size(max = 50) @NotBlank String username,
                                   @RequestParam @Size(max = 120) @Email @NotBlank String email,
                                   @RequestParam String password,
                                   @RequestParam @Size(max = 120) @NotBlank String fullName,
                                   @RequestParam @Size(max = 80) @NotBlank String department,
                                   @RequestParam(required = false) @Size(max = 30) String phone,
                                   @RequestParam @Size(max = 80) @NotBlank String specialization,
                                   @RequestParam(defaultValue = "5") @Min(1) int maxWorkload,
                                   @RequestParam(defaultValue = "2") @Min(0) int experienceYears,
                                   @RequestParam(defaultValue = "false") boolean teamLead,
                                   @RequestParam(required = false) List<Long> skillIds,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        userService.createTechnician(username, email, password, fullName, department, phone,
                specialization, maxWorkload, experienceYears, teamLead, skillIds, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "New technician onboarded successfully.");
        return "redirect:/admin/technicians";
    }

    @PostMapping("/technicians/{id}/update")
    public String updateTechnician(@PathVariable Long id,
                                   @RequestParam(defaultValue = "false") boolean available,
                                   @RequestParam @Min(1) int maxWorkload,
                                   @RequestParam(required = false) List<Long> skillIds,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        userService.updateTechnicianSkillsAndAvailability(id, available, maxWorkload, skillIds, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Technician capacity, availability, and skills updated.");
        return "redirect:/admin/technicians";
    }

    @GetMapping("/services")
    public String servicesManagementPage(Model model) {
        model.addAttribute("activePage", "admin-services");
        model.addAttribute("services", catalogService.getAllServices());
        model.addAttribute("criticalities", BusinessCriticality.values());
        return "admin/services";
    }

    @PostMapping("/services")
    public String createService(@RequestParam @Size(max = 40) @NotBlank String serviceCode,
                                @RequestParam @Size(max = 80) @NotBlank String name,
                                @RequestParam @Size(max = 255) @NotBlank String description,
                                @RequestParam BusinessCriticality businessCriticality,
                                @RequestParam @Size(max = 80) @NotBlank String ownerDepartment,
                                @RequestParam(defaultValue = "OPERATIONAL") @Size(max = 30) @NotBlank String operationalStatus,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        catalogService.createITService(serviceCode, name, description, businessCriticality,
                ownerDepartment, operationalStatus, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "IT Service added to enterprise catalog.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/status")
    public String updateServiceStatus(@PathVariable Long id,
                                      @RequestParam @Size(max = 30) @NotBlank String operationalStatus,
                                      @RequestParam BusinessCriticality businessCriticality,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        catalogService.updateServiceStatus(id, operationalStatus, businessCriticality, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "IT Service operational health and criticality updated.");
        return "redirect:/admin/services";
    }

    @GetMapping("/categories")
    public String categoriesManagementPage(Model model) {
        model.addAttribute("activePage", "admin-categories");
        model.addAttribute("categories", catalogService.getAllCategories());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam @Size(max = 40) @NotBlank String code,
                                 @RequestParam @Size(max = 80) @NotBlank String name,
                                 @RequestParam @Size(max = 255) @NotBlank String description,
                                 @RequestParam(defaultValue = "1.0") double defaultWeight,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        catalogService.createCategory(code, name, description, defaultWeight, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Incident category added successfully.");
        return "redirect:/admin/categories";
    }

    @GetMapping("/sla")
    public String slaConfigurationPage(Model model) {
        model.addAttribute("activePage", "admin-sla");
        model.addAttribute("slaPolicies", catalogService.getAllSLAPolicies());
        model.addAttribute("slaViolations", analyticsService.getRecentSLAViolations());
        return "admin/sla-config";
    }

    @PostMapping("/sla/{id}")
    public String updateSlaPolicy(@PathVariable Long id,
                                  @RequestParam @Min(1) int responseTargetMinutes,
                                  @RequestParam @Min(1) int resolutionTargetMinutes,
                                  @RequestParam @Min(5) @Max(90) int warningThresholdPercent,
                                  @RequestParam(defaultValue = "false") boolean escalationEnabled,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        catalogService.updateSLAPolicy(id, responseTargetMinutes, resolutionTargetMinutes,
                warningThresholdPercent, escalationEnabled, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "SLA Policy targets updated.");
        return "redirect:/admin/sla";
    }

    @GetMapping("/strategies")
    public String strategiesConfigurationPage(Model model) {
        model.addAttribute("activePage", "admin-strategies");
        model.addAttribute("assignmentStrategies", incidentService.getAllAssignmentStrategies());
        model.addAttribute("priorityStrategies", incidentService.getAllPriorityStrategies());
        model.addAttribute("activeAssignmentStrategy", incidentService.getActiveAssignmentStrategy().getStrategyCode());
        model.addAttribute("activePriorityStrategy", incidentService.getActivePriorityStrategy().getStrategyCode());
        model.addAttribute("configurations", catalogService.getAllConfigurations());
        return "admin/strategy-config";
    }

    @PostMapping("/strategies")
    @Transactional
    public String updateStrategies(@RequestParam String assignmentStrategy,
                                   @RequestParam String priorityStrategy,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        catalogService.updateConfigValue(CatalogService.CONFIG_ASSIGNMENT_STRATEGY, assignmentStrategy,
                "Active Technician Assignment Strategy", userDetails.getUsername());
        catalogService.updateConfigValue(CatalogService.CONFIG_PRIORITY_STRATEGY, priorityStrategy,
                "Active Priority Calculation Strategy", userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage",
                "Active Assignment (" + assignmentStrategy + ") and Priority (" + priorityStrategy + ") strategies updated.");
        return "redirect:/admin/strategies";
    }

    @GetMapping("/audit")
    public String auditLogsPage(@RequestParam(required = false) String actionType, Model model) {
        model.addAttribute("activePage", "admin-audit");
        model.addAttribute("auditLogs", auditService.getAuditLogsByActionType(actionType));
        model.addAttribute("selectedActionType", actionType != null ? actionType : "ALL");
        return "admin/audit-logs";
    }

    @GetMapping("/analytics")
    public String analyticsDashboardPage(Model model) {
        DashboardStatsDto stats = analyticsService.computeDashboardStats();
        Map<String, Long> categoryMap = analyticsService.getCategoryBreakdown();
        Map<String, Long> serviceMap = analyticsService.getServiceBreakdown();
        Map<String, Integer> workloadMap = analyticsService.getTechnicianWorkloadMap();

        model.addAttribute("activePage", "admin-analytics");
        model.addAttribute("stats", stats);
        model.addAttribute("technicians", userService.getAllTechnicians());
        model.addAttribute("services", catalogService.getAllServices());
        model.addAttribute("slaViolations", analyticsService.getRecentSLAViolations());
        model.addAttribute("categoryLabels", categoryMap.keySet());
        model.addAttribute("categoryValues", categoryMap.values());
        model.addAttribute("serviceLabels", serviceMap.keySet());
        model.addAttribute("serviceValues", serviceMap.values());
        model.addAttribute("workloadLabels", workloadMap.keySet());
        model.addAttribute("workloadValues", workloadMap.values());
        return "admin/analytics";
    }

    @GetMapping("/analytics/export.csv")
    public ResponseEntity<byte[]> exportAnalyticsCsv() {
        String csv = analyticsService.generateCsvExport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resolvr-incidents-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
