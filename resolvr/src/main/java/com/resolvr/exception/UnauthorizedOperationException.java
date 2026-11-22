package com.resolvr.exception;

/**
 * Thrown when a user attempts an action outside their role or ownership scope.
 */
public class UnauthorizedOperationException extends RuntimeException {
    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
