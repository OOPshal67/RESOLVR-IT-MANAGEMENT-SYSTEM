package com.resolvr.config;

import com.resolvr.security.CustomUserDetailsService;
import com.resolvr.security.LoginProvider;
import com.resolvr.security.RequestSecurityFilter;
import com.resolvr.security.RoleBasedAuthenticationSuccessHandler;
import com.resolvr.service.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RoleBasedAuthenticationSuccessHandler successHandler;

    public SecurityConfig(RoleBasedAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService users,
            JdbcTemplate db, AuditService audit) throws Exception {
        http.authenticationManager(new ProviderManager(
                    new LoginProvider(users, passwordEncoder(), db, audit)))
            .addFilterBefore(new RequestSecurityFilter(db),
                    CsrfFilter.class)
            .csrf(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
            .headers(h -> h.frameOptions(frame -> frame.deny()).referrerPolicy(ref -> ref.policy(
                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login", "/register", "/request-access", "/forgot-password", "/error",
                    "/css/**", "/js/**", "/images/**",
                    "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/technician/**").hasAnyAuthority("ROLE_TECHNICIAN", "ROLE_ADMIN")
                .requestMatchers("/employee/**").hasAuthority("ROLE_EMPLOYEE")
                .requestMatchers("/incidents/**", "/notifications/**", "/profile/**", "/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .addLogoutHandler((req, res, auth) -> {
                    if (auth != null) audit.recordSystemAction(auth.getName(), auth.getAuthorities().iterator().next().getAuthority(),
                            "SIGNED_OUT", "Signed out", null);
                })
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
