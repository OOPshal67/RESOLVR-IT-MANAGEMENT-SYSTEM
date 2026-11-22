package com.resolvr.security;

import com.resolvr.service.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginProvider extends DaoAuthenticationProvider {
    private final JdbcTemplate db;
    private final AuditService audit;

    public LoginProvider(CustomUserDetailsService users, PasswordEncoder encoder, JdbcTemplate db, AuditService audit) {
        setUserDetailsService(users);
        setPasswordEncoder(encoder);
        this.db = db;
        this.audit = audit;
    }

    @Override
    public Authentication authenticate(Authentication input) {
        String name = input.getName();
        if (Boolean.TRUE.equals(db.queryForObject(
                "select exists(select 1 from users where username=? and locked_until>now())", Boolean.class, name))) {
            throw new LockedException("Sign-in unavailable");
        }
        try {
            Authentication result = super.authenticate(input);
            int updated = db.update("""
                    update users set failures=0,locked_until=null,failed_at=null where username=?
                      and (locked_until is null or locked_until<=now()) and active
                    """, name);
            if (updated == 0) throw new LockedException("Sign-in unavailable");
            audit.recordSystemAction(name, result.getAuthorities().iterator().next().getAuthority(),
                    "SIGNED_IN", "Signed in", null);
            return result;
        } catch (BadCredentialsException ex) {
            var counts = db.queryForList("""
                    update users set failures=case when failed_at>now()-interval '15 minutes' then failures+1 else 1 end,
                    locked_until=case when failed_at>now()-interval '15 minutes' and failures>=4
                                      then now()+interval '15 minutes' end, failed_at=now()
                    where username=? and (locked_until is null or locked_until<=now()) returning failures
                    """, Integer.class, name);
            if (!counts.isEmpty()) {
                audit.recordSystemAction(name, "ACCOUNT", counts.get(0)>=5 ? "LOGIN_LOCKED" : "LOGIN_FAILED",
                        "Sign-in failed", null);
            }
            throw ex;
        }
    }
}
