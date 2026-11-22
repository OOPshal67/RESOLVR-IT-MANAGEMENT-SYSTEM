package com.resolvr.dto;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record AccountForm(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}") String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(max = 80) String department,
        @NotBlank @Size(min = 12, max = 72) String password) {

    @Override
    public String toString() {
        return "AccountForm[username=" + username + "]";
    }

    @AssertTrue(message = "Passwords must fit within 72 UTF-8 bytes")
    public boolean isPasswordLengthValid() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
