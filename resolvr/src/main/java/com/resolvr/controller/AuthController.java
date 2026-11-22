package com.resolvr.controller;

import com.resolvr.dto.RegistrationRequest;
import com.resolvr.entity.User;
import com.resolvr.enums.Role;
import com.resolvr.exception.InvalidIncidentException;
import com.resolvr.security.CustomUserDetails;
import com.resolvr.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    @PreAuthorize("permitAll()")
    public String home(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Role role = userDetails.getUser().getRole();
        if (role == Role.ROLE_ADMIN) return "redirect:/admin/dashboard";
        if (role == Role.ROLE_TECHNICIAN) return "redirect:/technician/dashboard";
        return "redirect:/employee/dashboard";
    }

    @GetMapping("/login")
    @PreAuthorize("permitAll()")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    @PreAuthorize("permitAll()")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public String registerEmployee(@Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerEmployee(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account registered successfully! You can now sign in with your credentials.");
            return "redirect:/login";
        } catch (InvalidIncidentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userService.getUserById(userDetails.getUser().getId());
        model.addAttribute("profileUser", user);
        model.addAttribute("activePage", "profile");
        return "auth/profile";
    }

    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam @Size(max = 120) @NotBlank String fullName,
                                @RequestParam @Size(max = 120) @Email @NotBlank String email,
                                @RequestParam @Size(max = 80) @NotBlank String department,
                                @RequestParam(required = false) @Size(max = 30) String phone,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(userDetails.getUser().getId());
        userService.updateUserProfile(user, fullName, email, department, phone);
        redirectAttributes.addFlashAttribute("successMessage", "Profile settings updated successfully.");
        return "redirect:/profile";
    }

    @GetMapping("/access-denied")
    @PreAuthorize("permitAll()")
    public String accessDenied(Model model) {
        model.addAttribute("statusCode", 403);
        model.addAttribute("errorTitle", "403 — Role Authorization Required");
        model.addAttribute("errorMessage", "Your account cannot open this page.");
        return "fragments/error";
    }
}
