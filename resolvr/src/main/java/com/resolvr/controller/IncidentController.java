package com.resolvr.controller;

import com.resolvr.entity.Incident;
import com.resolvr.enums.Role;
import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.IncidentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/incidents")
@PreAuthorize("isAuthenticated()")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/{id}")
    public String routeIncidentDetailByRole(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Incident incident = incidentService.getIncidentAuthorized(id, userDetails.getUser());
        Role role = userDetails.getUser().getRole();
        if (role == Role.ROLE_EMPLOYEE) {
            return "redirect:/employee/incidents/" + incident.getId();
        }
        return "redirect:/technician/incidents/" + incident.getId();
    }
}
