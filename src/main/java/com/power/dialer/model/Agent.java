package com.power.dialer.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder(toBuilder = true)
@Value
public class Agent {

    /* AVAILABLE: When an agent is logged in and is ready to accept a call
     * WAITING_TO_BE_ENGAGED: When an agent is waiting for call to be picked up
     * ENGAGED: When an agent is engaged in a call
     * OFF_DUTY: When an agent is not currently active
     */
    public enum AgentStatus {
        AVAILABLE,
        WAITING_TO_BE_ENGAGED,
        ENGAGED,
        OFF_DUTY
    }

    @NonNull
    private String agentId;
    @NonNull
    private AgentStatus agentStatus;

    public AgentRecord toRecord() {
        return AgentRecord.builder()
            .agentId(this.agentId)
            .agentStatus(this.agentStatus.toString())
            .build();
    }
}
