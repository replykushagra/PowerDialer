package com.power.dialer.model;

import java.util.Map;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Lead {

    /*
     * AVAILABLE: A lead is available to be queued in any agent's queue
     * QUEUED: A lead is assigned to an agent, not ready to be called yet
     * WAITING_TO_BE_ENGAGED: The call to lead is ringing/in progress, but the lead hasn't picked it yet
     * ENGAGED: An agent is communication with the lead
     * COMPLETED: Indicated a successful completion of the call between an agent and a lead
     */
    public enum LeadStatus {
        AVAILABLE,
        QUEUED,
        ENGAGED,
        WAITING_TO_BE_ENGAGED,
        COMPLETED,
    }

    @NonNull
    private final String phoneNumber;
    @NonNull
    private final LeadStatus currentStatus;

    // By default a Lead is assigned to "NONE"
    @NonNull
    private final String agentId;

    // This could be used by to keep track of customer data/preferences 
    private Map<String, String> metadata;

    public LeadRecord toLeadRecord() {
        return LeadRecord.builder()
            .agentId(this.agentId)
            .phoneNumber(this.phoneNumber)
            .status(this.currentStatus.toString())
            .metadata(this.metadata)
            .build();
    }
}
