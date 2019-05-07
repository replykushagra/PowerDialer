package com.power.dialer.exception;

/*
 * Error thrown when we run out of leads. Clients are encouraged to retry with backoff/jitter on this error
 */
public class NoLeadsAvailableException extends RuntimeException {

    public NoLeadsAvailableException(final String message) {
        super(message);
    }

    public NoLeadsAvailableException(final String message, final Throwable t) {
        super(message, t);
    }

}
