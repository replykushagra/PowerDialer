package com.power.dialer.compute;

import com.power.dialer.exception.AgentSessionTerminationException;

/*
 * The PowerDialer interface allows us to perform agent actions. Ideally it should be hooked up
 * with a lambda handler which listens to the user actions and performs retries/error handling
 * for the APIs implemented here
 */
public interface PowerDialer {

    /**
     * Action when an Agent logs in. Automatically pulls in 2 leads in the Agent's queue and dials one of them 
     * @param agentId agent id
     */
    void onAgentLogin(final String agentId);

    /**
     * Action when an Agent logs out. Checks if the Agent's status is not ENGAGED and resets all the pending leads in the Agen'ts queue to make them available in the pool
     * @param agentId agent id
     * @throws AgentSessionTerminationException error thrown when the Agent's status is ENGAGED
     */
    void onAgentLogout(final String agentId) throws AgentSessionTerminationException;

    /**
     * Action when a communication channel is established between agent and the lead. Updates the status of both agent and lead
     * @param agentId
     * @param phoneNumber
     */
    void onCallStarted(final String agentId, final String phoneNumber);

    /**
     * Action when either a network connection breaks or any other unforeseen error which results in an unsuccessful termination of a call
     * between agent and lead. It takes care of replenishing the Agent's queue with a new lead
     * @param agentId
     * @param phoneNumber
     */
    void onCallFailed(final String agentId, final String phoneNumber);

    /**
     * Action when a call successfully completed. Updates the status of lead and agent. Also replenishes the Agent's queue with a new lead
     * @param agentId
     * @param phoneNumber
     */
    void onCallEnded(final String agentId, final String phoneNumber);
}
