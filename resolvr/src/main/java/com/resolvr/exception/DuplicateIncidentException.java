package com.resolvr.exception;

/**
 * Thrown when an explicit duplicate constraint or conflict is triggered.
 */
public class DuplicateIncidentException extends RuntimeException {
    public DuplicateIncidentException(String message) {
        super(message);
    }

    public DuplicateIncidentException(String message, Throwable cause) {
        super(message, cause);
    }
}
