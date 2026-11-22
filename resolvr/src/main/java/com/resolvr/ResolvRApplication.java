package com.resolvr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResolvRApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResolvRApplication.class, args);
    }
}
