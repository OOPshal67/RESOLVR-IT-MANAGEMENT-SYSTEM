package com.resolvr.config;

import com.resolvr.entity.User;
import com.resolvr.enums.Role;
import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.AnalyticsService;
import com.resolvr.service.IncidentService;
import com.resolvr.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributesAdvice {

    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final IncidentService incidentService;

    public GlobalModelAttributesAdvice(NotificationService notificationService,
                                       AnalyticsService analyticsService,
                                       IncidentService incidentService) {
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.incidentService = incidentService;
    }

    @ModelAttribute
    public void populateGlobalAttributes(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            User currentUser = userDetails.getUser();
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("unreadNotificationCount", notificationService.getUnreadCount(currentUser));
            if (currentUser.getRole() != Role.ROLE_ADMIN) return;
            model.addAttribute("sidebarViolations", analyticsService.getRecentSLAViolations().stream().limit(2).toList());
            model.addAttribute("sidebarRecentIncidents", incidentService.getRecentIncidents().stream().limit(2).toList());
        }
    }
}
