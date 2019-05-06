package com.power.dialer.exception;

/*
 * When service fails to connect to lead
 */
public class CallToLeadFailedException extends RuntimeException {

    public CallToLeadFailedException(final String message) {
        super(message);
    }

    public CallToLeadFailedException(final String message, final Throwable t) {
        super(message, t);
    }
}
