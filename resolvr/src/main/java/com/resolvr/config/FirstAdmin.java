package com.resolvr.config;

import com.resolvr.dto.AccountForm;
import com.resolvr.service.AccountAccessService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class FirstAdmin implements CommandLineRunner {
    private final AccountAccessService accounts;
    private final Environment env;

    public FirstAdmin(AccountAccessService accounts, Environment env) {
        this.accounts = accounts;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        String password = env.getProperty("BOOTSTRAP_PASSWORD", "");
        if (!password.isEmpty()) {
            accounts.bootstrap(new AccountForm(env.getProperty("BOOTSTRAP_USERNAME"), env.getProperty("BOOTSTRAP_EMAIL"),
                    "Service desk administrator", "IT", password));
        }
    }
}
