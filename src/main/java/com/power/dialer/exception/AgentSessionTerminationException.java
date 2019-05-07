package com.power.dialer.exception;

/*
 * Error thrown when agent tries to terminate the session when in ENGAGED status
 */
public class AgentSessionTerminationException extends RuntimeException {

    public AgentSessionTerminationException(final String message) {
        super(message);
    }

    public AgentSessionTerminationException(final String message, final Throwable t) {
        super(message, t);
    }
}
