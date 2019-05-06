package com.power.dialer.exception;

public class AgentSessionTerminationException extends RuntimeException {

    public AgentSessionTerminationException(final String message) {
        super(message);
    }

    public AgentSessionTerminationException(final String message, final Throwable t) {
        super(message, t);
    }
}
