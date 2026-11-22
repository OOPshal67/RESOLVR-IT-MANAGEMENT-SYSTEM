package com.resolvr.security;

import com.resolvr.enums.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        boolean isTechnician = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_TECHNICIAN.name()));

        String selected = request.getParameter("workspace");
        if (selected != null && authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_" + selected))) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.sendRedirect(request.getContextPath() + "/login?role");
            return;
        }
        if (isAdmin) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } else if (isTechnician) {
            response.sendRedirect(request.getContextPath() + "/technician/dashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/employee/dashboard");
        }
    }
}
