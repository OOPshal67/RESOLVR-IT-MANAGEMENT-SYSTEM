package com.resolvr;

import com.resolvr.dto.AccountForm;
import com.resolvr.exception.InvalidIncidentException;
import com.resolvr.service.AccountAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountWorkflowIT {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
        properties.add("BOOTSTRAP_PASSWORD", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired AccountAccessService accounts;
    @Autowired JdbcTemplate db;

    private AccountForm applicant() {
        String username = "test-" + UUID.randomUUID().toString().substring(0, 10);
        return new AccountForm(username, username + "@example.org", "Lab operator", "IT", UUID.randomUUID().toString());
    }

    @Test
    void approvalAndPasswordRecoveryCanOnlyHappenOnce() {
        var form = applicant();
        accounts.request(form);
        long id = db.queryForObject("select id from access_requests where username=?", Long.class, form.username());
        assertEquals(0, db.queryForObject("select count(*) from users where username=?", Integer.class, form.username()).intValue());
        accounts.decide(id, true, "reviewer");
        assertThrows(InvalidIncidentException.class, () -> accounts.decide(id, true, "reviewer"));
        long userId = db.queryForObject("select id from users where username=? and role='ROLE_TECHNICIAN'", Long.class, form.username());
        String code = accounts.issueReset(userId, "reviewer");
        accounts.reset(code, UUID.randomUUID().toString());
        assertThrows(InvalidIncidentException.class, () -> accounts.reset(code, UUID.randomUUID().toString()));
        assertTrue(db.queryForObject("select password_hash from users where id=?", String.class, userId).startsWith("$2a$12$"));
        assertEquals(1, db.queryForObject("select count(*) from audit_logs where action_type='PASSWORD_RESET' and actor_username=?",
                Integer.class, form.username()).intValue());
    }

    @Test
    void rejectionNeverCreatesAUser() {
        var form = applicant();
        accounts.request(form);
        long id = db.queryForObject("select id from access_requests where username=?", Long.class, form.username());
        accounts.decide(id, false, "reviewer");
        assertNull(db.queryForObject("select password_hash from access_requests where id=?", String.class, id));
        assertThrows(InvalidIncidentException.class, () -> accounts.decide(id, true, "reviewer"));
        assertEquals(0, db.queryForObject("select count(*) from users where username=?", Integer.class, form.username()).intValue());
    }

    @Test
    void formsRequireCsrfAndAdminReviewRequiresAnAdmin() throws Exception {
        mvc.perform(post("/register")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/access/1").param("approve", "true").with(csrf()).with(user("staff").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        for (String path : new String[]{"/login","/register","/request-access","/forgot-password"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
        }
        mvc.perform(get("/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    void publicRegistrationCannotSelectAdminRoleAndLockoutExpires() throws Exception {
        var form = applicant();
        mvc.perform(post("/register").with(csrf()).param("username", form.username()).param("email", form.email())
                .param("password", form.password()).param("fullName", form.fullName()).param("department", "IT")
                .param("officeLocation", "Lab").param("jobTitle", "Instructor").param("role", "ROLE_ADMIN"))
                .andExpect(redirectedUrl("/login"));
        assertEquals("ROLE_EMPLOYEE", db.queryForObject("select role from users where username=?", String.class, form.username()));
        for (int i=0; i<5; i++) login(form.username(), "incorrect", "/login?error=true");
        login(form.username(), form.password(), "/login?error=true");
        db.update("update users set locked_until=now()-interval '1 second' where username=?", form.username());
        login(form.username(), form.password(), "/employee/dashboard");
    }

    @Test
    void bootstrapIsConsumedEvenWhenRepeated() {
        var first = applicant();
        var second = applicant();
        accounts.bootstrap(first);
        accounts.bootstrap(second);
        assertEquals(1, db.queryForObject("select count(*) from first_admin", Integer.class).intValue());
        assertEquals(0, db.queryForObject("select count(*) from users where username=?", Integer.class, second.username()).intValue());
    }

    @Test
    void upgradesAnExistingSchemaAndPreservesItsIncidentNumberRange() throws Exception {
        String schema = "legacy_upgrade";
        org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).target("2").load().migrate();
        try (var connection = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("set search_path to legacy_upgrade");
            sql.executeUpdate("""
                    insert into users(username,email,password_hash,full_name,role,department)
                    values('admin','admin@resolvr.io','old-hash','Legacy admin','ROLE_ADMIN','IT')
                    """);
            org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .schemas(schema).load().migrate();
            try (var result = sql.executeQuery("""
                    select password_hash, (select count(*) from audit_logs where action_type='LEGACY_CREDENTIAL_RETIRED'),
                           (select count(*) from sla_policies), nextval('incident_number') from users where username='admin'
                    """)) {
                assertTrue(result.next());
                assertEquals("!retired", result.getString(1));
                assertEquals(1, result.getInt(2));
                assertEquals(4, result.getInt(3));
                assertTrue(result.getLong(4) > 1016);
            }
        }
    }

    @Test
    void expiredRecoveryCodeCannotBeUsed() {
        var form = applicant();
        accounts.request(form);
        long requestId = db.queryForObject("select id from access_requests where username=?", Long.class, form.username());
        accounts.decide(requestId, true, "reviewer");
        long id = db.queryForObject("select id from users where username=?", Long.class, form.username());
        String code = accounts.issueReset(id, "reviewer");
        db.update("update users set reset_until=now()-interval '1 second' where id=?", id);
        assertThrows(InvalidIncidentException.class, () -> accounts.reset(code, UUID.randomUUID().toString()));
    }

    private void login(String name, String password, String destination) throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", name).param("password", password))
                .andExpect(redirectedUrl(destination));
    }
}
