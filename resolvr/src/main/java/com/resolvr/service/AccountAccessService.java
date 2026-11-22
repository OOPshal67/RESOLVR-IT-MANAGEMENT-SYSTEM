package com.resolvr.service;

import com.resolvr.dto.AccountForm;
import com.resolvr.entity.Administrator;
import com.resolvr.entity.Technician;
import com.resolvr.entity.User;
import com.resolvr.exception.InvalidIncidentException;
import com.resolvr.repository.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
public class AccountAccessService {
    private final JdbcTemplate db;
    private final UserRepository users;
    private final TechnicianRepository technicians;
    private final AdministratorRepository admins;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AccountAccessService(JdbcTemplate db, UserRepository users, TechnicianRepository technicians,
                                AdministratorRepository admins, PasswordEncoder encoder, AuditService audit) {
        this.db = db;
        this.users = users;
        this.technicians = technicians;
        this.admins = admins;
        this.encoder = encoder;
        this.audit = audit;
    }

    public void request(@Valid AccountForm form) {
        checkAvailable(form);
        db.update("""
                insert into access_requests(username,email,full_name,department,password_hash,status)
                values (?,?,?,?,?,'PENDING')
                """, form.username(), form.email(), form.fullName(), form.department(), encoder.encode(form.password()));
        audit.recordSystemAction(form.username(), "APPLICANT", "ACCESS_REQUESTED", "Requested technician access", null);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> pending() {
        return db.queryForList("select id,username,email,full_name,department from access_requests where status='PENDING' order by id");
    }

    public void decide(long id, boolean approve, String actor) {
        var rows = db.queryForList("select * from access_requests where id=? and status='PENDING' for update", id);
        if (rows.isEmpty()) throw new InvalidIncidentException("This request has already been reviewed.");
        var row = rows.get(0);
        String username = (String) row.get("username");
        if (approve) {
            technicians.save(new Technician(username, (String) row.get("email"), (String) row.get("password_hash"),
                    (String) row.get("full_name"), (String) row.get("department"), null,
                    "TECH" + technicians.nextTechnicianNumber(), "General support", 5, 0, false));
        }
        String decision = approve ? "APPROVED" : "REJECTED";
        db.update("update access_requests set status=?, password_hash=null where id=?", decision, id);
        audit.recordSystemAction(actor, "ROLE_ADMIN", "ACCESS_" + decision, username, null);
    }

    public void createAdmin(@Valid AccountForm form, String actor) {
        checkAvailable(form);
        admins.save(new Administrator(form.username(), form.email(), encoder.encode(form.password()), form.fullName(),
                form.department(), null, "ADM" + UUID.randomUUID().toString().substring(0, 18), "FULL_SYSTEM", "GLOBAL"));
        audit.recordSystemAction(actor, actor.equals("SYSTEM") ? "SYSTEM" : "ROLE_ADMIN",
                "ADMIN_CREATED", form.username(), null);
    }

    public void bootstrap(@Valid AccountForm form) {
        if (db.update("insert into first_admin values(1) on conflict do nothing") == 0) return;
        if (db.queryForObject("select count(*) from users where role='ROLE_ADMIN' and active and password_hash <> '!retired'", Long.class) == 0) {
            createAdmin(form, "SYSTEM");
        }
    }

    private void checkAvailable(AccountForm form) {
        if (users.existsByUsername(form.username()) || users.existsByEmail(form.email())) {
            throw new InvalidIncidentException("The account details are already in use.");
        }
    }

    public String issueReset(long userId, String actor) {
        User user = users.findById(userId).orElseThrow(() -> new InvalidIncidentException("Unknown account."));
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        db.update("update users set reset_digest=?,reset_until=now()+interval '15 minutes' where id=?", digest(token), userId);
        audit.recordSystemAction(actor, "ROLE_ADMIN", "RESET_ISSUED", user.getUsername(), null);
        return token;
    }

    public void reset(String token, String password) {
        requirePassword(password);
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidReset();
        String hash = digest(token);
        if (!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from users where reset_digest=? and reset_until>now())",
                Boolean.class, hash))) throw invalidReset();
        var names = db.queryForList("""
                update users set password_hash=?,reset_digest=null,reset_until=null,failures=0,
                  locked_until=null,revision=revision+1 where reset_digest=? and reset_until>now()
                returning username
                """, String.class, encoder.encode(password), hash);
        if (names.isEmpty()) throw invalidReset();
        audit.recordSystemAction(names.get(0), "ACCOUNT_OWNER", "PASSWORD_RESET", "Used recovery code", null);
    }

    private static InvalidIncidentException invalidReset() {
        return new InvalidIncidentException("This recovery code is invalid or expired.");
    }

    public static void requirePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 12 ||
                password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidIncidentException("Use at least 12 characters and at most 72 UTF-8 bytes.");
        }
    }

    private static String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
