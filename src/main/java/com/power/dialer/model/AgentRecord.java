package com.power.dialer.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.power.dialer.model.Agent.AgentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
@AllArgsConstructor
@DynamoDBTable(tableName = "agent")
public class AgentRecord {

    public static final String PARTITION_KEY_ATTRIBUTE_NAME = "agentId";

    @NonNull private String agentId;
    @NonNull private String status;

    @DynamoDBHashKey(attributeName = PARTITION_KEY_ATTRIBUTE_NAME)
    public String getAgentId() {
        return this.agentId;
    }

    @DynamoDBAttribute
    public String getStatus() {
        return this.status;
    }

    public Agent toAgent() {
        return Agent.builder()
            .agentId(this.getAgentId())
            .status(AgentStatus.valueOf(this.getStatus()))
            .build();
    }
}
