package com.power.dialer.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder(toBuilder = true)
@Value
public class Agent {

    /* AVAILABLE: When an agent who is logged in and is ready to make a call
     * WAITING_TO_BE_ENGAGED: When an agent is waiting for call to be picked up
     * ENGAGED: When an agent is engaged in a call
     * OFF_DUTY: When an agent is not current active
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
    private AgentStatus status;

    public AgentRecord toRecord() {
        return AgentRecord.builder()
            .agentId(this.agentId)
            .status(this.status.toString())
            .build();
    }
}
