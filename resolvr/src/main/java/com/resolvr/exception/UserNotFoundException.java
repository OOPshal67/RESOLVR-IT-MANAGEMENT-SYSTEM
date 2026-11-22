package com.resolvr.exception;

/**
 * Thrown when a user, employee, technician, or administrator record cannot be found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
