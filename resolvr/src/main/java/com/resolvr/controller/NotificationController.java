package com.resolvr.controller;

import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String notificationsPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("activePage", "notifications");
        model.addAttribute("notifications", notificationService.getNotificationsForUser(userDetails.getUser()));
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markNotificationAsRead(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {
        notificationService.markAsRead(id, userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllNotificationsAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             RedirectAttributes redirectAttributes) {
        notificationService.markAllAsRead(userDetails.getUser());
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
