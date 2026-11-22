package com.resolvr.controller;

import com.resolvr.dto.AccountForm;
import com.resolvr.service.AccountAccessService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AccountAccessController {
    private final AccountAccessService accounts;

    public AccountAccessController(AccountAccessService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/request-access")
    @PreAuthorize("permitAll()")
    public String requestForm(Model model) {
        model.addAttribute("accountForm", new AccountForm("", "", "", "", ""));
        return "auth/request-access";
    }

    @PostMapping("/request-access")
    @PreAuthorize("permitAll()")
    public String request(@Valid @ModelAttribute AccountForm form, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) return "auth/request-access";
        accounts.request(form);
        flash.addFlashAttribute("successMessage", "Request received. An administrator will review it.");
        return "redirect:/login";
    }

    @GetMapping("/admin/access")
    public String review(Model model) {
        model.addAttribute("requests", accounts.pending());
        return "admin/access";
    }

    @PostMapping("/admin/access/{id}")
    public String decide(@PathVariable long id, @RequestParam boolean approve, Principal actor) {
        accounts.decide(id, approve, actor.getName());
        return "redirect:/admin/access";
    }

    @PostMapping("/admin/accounts")
    public String createAdmin(@Valid @ModelAttribute AccountForm form, Principal actor) {
        accounts.createAdmin(form, actor.getName());
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/recovery")
    public String issueReset(@PathVariable long id, Principal actor, Model model) {
        model.addAttribute("token", accounts.issueReset(id, actor.getName()));
        return "admin/recovery";
    }

    @GetMapping("/forgot-password")
    @PreAuthorize("permitAll()")
    public String recovery() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    @PreAuthorize("permitAll()")
    public String recover(@RequestParam String token, @RequestParam String password, RedirectAttributes flash) {
        accounts.reset(token, password);
        flash.addFlashAttribute("successMessage", "Password changed. Sign in with your new password.");
        return "redirect:/login";
    }
}
