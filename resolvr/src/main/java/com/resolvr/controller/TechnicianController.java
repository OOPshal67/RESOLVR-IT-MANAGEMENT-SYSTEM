package com.resolvr.controller;

import com.resolvr.dto.IncidentSearchCriteria;
import com.resolvr.dto.ResolutionRequest;
import com.resolvr.entity.Incident;
import com.resolvr.entity.Technician;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.enums.Role;
import com.resolvr.enums.SLARiskLevel;
import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/technician")
@PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
public class TechnicianController {

    private final IncidentService incidentService;
    private final UserService userService;
    private final CatalogService catalogService;
    private final AuditService auditService;
    private final DuplicateDetectionService duplicateDetectionService;

    public TechnicianController(IncidentService incidentService,
                                UserService userService,
                                CatalogService catalogService,
                                AuditService auditService,
                                DuplicateDetectionService duplicateDetectionService) {
        this.incidentService = incidentService;
        this.userService = userService;
        this.catalogService = catalogService;
        this.auditService = auditService;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Incident> assigned;
        Technician tech = null;
        if (userDetails.getUser().getRole() == Role.ROLE_TECHNICIAN) {
            tech = userService.getTechnicianByUsername(userDetails.getUsername());
            assigned = incidentService.getIncidentsForTechnician(tech);
        } else {
            assigned = incidentService.getAllIncidents();
        }

        long activeCount = assigned.stream().filter(i -> i.getStatus().isActive()).count();
        long unackCount = assigned.stream().filter(i -> i.getStatus() == IncidentStatus.ASSIGNED).count();
        long atRiskCount = assigned.stream().filter(i ->
                i.getSlaRiskLevel() == SLARiskLevel.HIGH ||
                i.getSlaRiskLevel() == SLARiskLevel.CRITICAL ||
                i.getSlaRiskLevel() == SLARiskLevel.BREACHED).count();
        long resolvedCount = assigned.stream().filter(i ->
                i.getStatus() == IncidentStatus.RESOLVED ||
                i.getStatus() == IncidentStatus.VERIFIED ||
                i.getStatus() == IncidentStatus.CLOSED).count();

        model.addAttribute("activePage", "tech-dashboard");
        model.addAttribute("technician", tech);
        model.addAttribute("assignedIncidents", assigned);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("unackCount", unackCount);
        model.addAttribute("atRiskCount", atRiskCount);
        model.addAttribute("resolvedCount", resolvedCount);
        return "technician/dashboard";
    }

    @GetMapping("/incidents")
    public String assignedIncidents(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @Valid @ModelAttribute("criteria") IncidentSearchCriteria criteria,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) Long selectedId,
                                    Model model) {
        if (userDetails.getUser().getRole() == Role.ROLE_TECHNICIAN) {
            criteria.setTechnicianId(userDetails.getUser().getId());
        }
        Page<Incident> incidentPage = incidentService.searchIncidents(criteria, userDetails.getUser(), page, size);
        Incident selectedIncident = null;
        if (selectedId != null) {
            selectedIncident = incidentService.getIncidentAuthorized(selectedId, userDetails.getUser());
        } else if (!incidentPage.getContent().isEmpty()) {
            selectedIncident = incidentService.getIncidentAuthorized(incidentPage.getContent().get(0).getId(), userDetails.getUser());
        }

        model.addAttribute("activePage", "tech-assigned");
        model.addAttribute("incidentPage", incidentPage);
        model.addAttribute("selectedIncident", selectedIncident);
        if (selectedIncident != null) {
            model.addAttribute("selectedAuditLogs", auditService.getAuditLogsForIncident(selectedIncident));
            model.addAttribute("selectedRelationships", duplicateDetectionService.getVisibleRelationships(selectedIncident, userDetails.getUser()));
        }
        model.addAttribute("categories", catalogService.getAllCategories());
        model.addAttribute("services", catalogService.getAllServices());
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", PriorityLevel.values());
        return "technician/assigned-incidents";
    }

    @GetMapping("/incidents/{id}")
    public String investigationWorkspace(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         Model model) {
        Incident incident = incidentService.getIncidentAuthorized(id, userDetails.getUser());
        model.addAttribute("activePage", "tech-assigned");
        model.addAttribute("incident", incident);
        model.addAttribute("auditLogs", auditService.getAuditLogsForIncident(incident));
        model.addAttribute("relationships", duplicateDetectionService.getVisibleRelationships(incident, userDetails.getUser()));
        model.addAttribute("technicians", userService.getAllTechnicians());
        model.addAttribute("resolutionRequest", new ResolutionRequest());
        return "technician/investigation";
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public String acknowledgeIncident(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        incidentService.acknowledgeIncident(id, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Incident acknowledged and first response SLA recorded.");
        return "redirect:/technician/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/start")
    public String startInvestigation(@PathVariable Long id,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        incidentService.startInvestigation(id, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Incident transitioned to IN_PROGRESS.");
        return "redirect:/technician/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/note")
    public String addInvestigationNote(@PathVariable Long id,
                                       @RequestParam @Size(max = 4000) @NotBlank String noteContent,
                                       @RequestParam(defaultValue = "false") boolean internalOnly,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        incidentService.addInvestigationNote(id, noteContent, internalOnly, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage",
                internalOnly ? "Internal engineering note saved." : "Public reply posted to reporter thread.");
        return "redirect:/technician/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/request-info")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public String requestInformation(@PathVariable Long id,
                                     @RequestParam @Size(max = 4000) @NotBlank String requestMessage,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        Technician tech = userService.getTechnicianByUsername(userDetails.getUsername());
        incidentService.requestAdditionalInformation(id, requestMessage, tech);
        redirectAttributes.addFlashAttribute("successMessage",
                "Information request sent to employee. Status updated to WAITING_FOR_USER.");
        return "redirect:/technician/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/resolve")
    public String resolveIncident(@PathVariable Long id,
                                  @Valid @ModelAttribute ResolutionRequest resolutionRequest,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        incidentService.resolveIncident(id, resolutionRequest, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage",
                "Incident marked as RESOLVED. Reporter has been notified for verification.");
        return "redirect:/technician/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/reassignment")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public String requestReassignment(@PathVariable Long id,
                                      @RequestParam @Size(max = 255) @NotBlank String reason,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        Technician tech = userService.getTechnicianByUsername(userDetails.getUsername());
        incidentService.requestReassignment(id, reason, tech);
        redirectAttributes.addFlashAttribute("successMessage", "Reassignment request submitted to IT Administration.");
        return "redirect:/technician/dashboard";
    }
}
