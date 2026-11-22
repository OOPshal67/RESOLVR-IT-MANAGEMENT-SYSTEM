package com.resolvr.exception;

/**
 * Thrown when no SLA policy is configured for a calculated priority level.
 */
public class MissingSLAPolicyException extends RuntimeException {
    public MissingSLAPolicyException(String message) {
        super(message);
    }

    public MissingSLAPolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}
