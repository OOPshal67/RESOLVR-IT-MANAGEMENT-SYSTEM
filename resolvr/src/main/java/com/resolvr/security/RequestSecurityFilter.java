package com.resolvr.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestSecurityFilter extends OncePerRequestFilter {
    private final JdbcTemplate db;

    public RequestSecurityFilter(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String nonce = UUID.randomUUID().toString();
        req.setAttribute("cspNonce", nonce);
        res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net 'nonce-" +
                nonce + "'; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net;" +
                " img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details &&
                !Boolean.TRUE.equals(db.queryForObject(
                "select exists(select 1 from users where id=? and active and password_hash=? and role=?)",
                Boolean.class, details.getUser().getId(), details.getPassword(), details.getUser().getRole().name()))) {
            if (req.getSession(false) != null) req.getSession(false).invalidate();
            SecurityContextHolder.clearContext();
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        chain.doFilter(req, res);
    }
}
