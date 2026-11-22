package com.resolvr.controller;

import com.resolvr.dto.FeedbackRequest;
import com.resolvr.dto.IncidentCreateRequest;
import com.resolvr.dto.IncidentSearchCriteria;
import com.resolvr.entity.Employee;
import com.resolvr.entity.Incident;
import com.resolvr.enums.ImpactLevel;
import com.resolvr.enums.IncidentStatus;
import com.resolvr.enums.PriorityLevel;
import com.resolvr.enums.UrgencyLevel;
import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.AuditService;
import com.resolvr.service.CatalogService;
import com.resolvr.service.DuplicateDetectionService;
import com.resolvr.service.IncidentService;
import com.resolvr.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employee")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {

    private final IncidentService incidentService;
    private final CatalogService catalogService;
    private final UserService userService;
    private final AuditService auditService;
    private final DuplicateDetectionService duplicateDetectionService;

    public EmployeeController(IncidentService incidentService,
                              CatalogService catalogService,
                              UserService userService,
                              AuditService auditService,
                              DuplicateDetectionService duplicateDetectionService) {
        this.incidentService = incidentService;
        this.catalogService = catalogService;
        this.userService = userService;
        this.auditService = auditService;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Employee employee = userService.getEmployeeByUsername(userDetails.getUsername());
        List<Incident> myIncidents = incidentService.getIncidentsForEmployee(employee);

        long total = myIncidents.size();
        long active = myIncidents.stream().filter(i -> i.getStatus().isActive()).count();
        long awaitingVerify = myIncidents.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED).count();
        long closed = myIncidents.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED || i.getStatus() == IncidentStatus.VERIFIED).count();

        model.addAttribute("activePage", "employee-dashboard");
        model.addAttribute("myIncidents", myIncidents);
        model.addAttribute("totalCount", total);
        model.addAttribute("activeCount", active);
        model.addAttribute("awaitingVerifyCount", awaitingVerify);
        model.addAttribute("closedCount", closed);
        return "employee/dashboard";
    }

    @GetMapping("/incidents/new")
    public String createIncidentForm(Model model) {
        model.addAttribute("activePage", "create-incident");
        model.addAttribute("incidentRequest", new IncidentCreateRequest());
        model.addAttribute("categories", catalogService.getActiveCategories());
        model.addAttribute("services", catalogService.getActiveServices());
        model.addAttribute("impactLevels", ImpactLevel.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        return "employee/create-incident";
    }

    @PostMapping("/incidents")
    public String submitIncident(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid @ModelAttribute("incidentRequest") IncidentCreateRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "create-incident");
            model.addAttribute("categories", catalogService.getActiveCategories());
            model.addAttribute("services", catalogService.getActiveServices());
            model.addAttribute("impactLevels", ImpactLevel.values());
            model.addAttribute("urgencyLevels", UrgencyLevel.values());
            return "employee/create-incident";
        }
        Employee reporter = userService.getEmployeeByUsername(userDetails.getUsername());
        Incident created = incidentService.createIncident(request, reporter);
        redirectAttributes.addFlashAttribute("successMessage",
                "Incident " + created.getIncidentKey() + " reported! Priority calculated as " +
                created.getPriority().getLabel() + ".");
        return "redirect:/employee/incidents/" + created.getId();
    }

    @GetMapping("/incidents")
    public String incidentHistory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @Valid @ModelAttribute("criteria") IncidentSearchCriteria criteria,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) Long selectedId,
                                  Model model) {
        Page<Incident> incidentPage = incidentService.searchIncidents(criteria, userDetails.getUser(), page, size);
        Incident selectedIncident = null;
        if (selectedId != null) {
            selectedIncident = incidentService.getIncidentAuthorized(selectedId, userDetails.getUser());
        } else if (!incidentPage.getContent().isEmpty()) {
            selectedIncident = incidentPage.getContent().get(0);
        }

        model.addAttribute("activePage", "employee-history");
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
        return "employee/history";
    }

    @GetMapping("/incidents/{id}")
    public String viewIncidentDetails(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        Incident incident = incidentService.getIncidentAuthorized(id, userDetails.getUser());
        model.addAttribute("activePage", "employee-history");
        model.addAttribute("incident", incident);
        model.addAttribute("auditLogs", auditService.getAuditLogsForIncident(incident));
        model.addAttribute("relationships", duplicateDetectionService.getVisibleRelationships(incident, userDetails.getUser()));
        model.addAttribute("feedbackRequest", new FeedbackRequest());
        return "employee/incident-details";
    }

    @PostMapping("/incidents/{id}/comment")
    public String addEmployeeComment(@PathVariable Long id,
                                     @RequestParam @Size(max = 4000) @NotBlank String content,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        incidentService.addInvestigationNote(id, content, false, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Your update has been added to the incident thread.");
        return "redirect:/employee/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/respond")
    public String respondToInfoRequest(@PathVariable Long id,
                                       @RequestParam @Size(max = 4000) @NotBlank String responseText,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        Employee reporter = userService.getEmployeeByUsername(userDetails.getUsername());
        incidentService.respondToInformationRequest(id, responseText, reporter);
        redirectAttributes.addFlashAttribute("successMessage",
                "Additional information submitted. Incident moved back to active investigation.");
        return "redirect:/employee/incidents/" + id;
    }

    @PostMapping("/incidents/{id}/verify-close")
    public String verifyAndClose(@PathVariable Long id,
                                 @Valid @ModelAttribute FeedbackRequest feedbackRequest,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        incidentService.verifyAndCloseIncident(id, feedbackRequest, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage",
                "Resolution verified and incident closed! Thank you for submitting your rating.");
        return "redirect:/employee/incidents/" + id;
    }
}
