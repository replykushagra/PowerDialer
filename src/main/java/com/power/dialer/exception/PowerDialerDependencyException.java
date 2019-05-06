package com.power.dialer.exception;

/**
 * Error thrown for dependency errors. Could be retryable
 */
public class PowerDialerDependencyException extends RuntimeException {

    public PowerDialerDependencyException(final String message) {
        super(message);
    }

    public PowerDialerDependencyException(final String message, final Throwable t) {
        super(message, t);
    }
}
