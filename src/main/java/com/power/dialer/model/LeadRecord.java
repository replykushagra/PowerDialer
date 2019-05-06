package com.power.dialer.model;

import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.power.dialer.model.Lead.LeadStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
@AllArgsConstructor
@DynamoDBTable(tableName = "lead")
public class LeadRecord {

    public static final String PARTITION_KEY_ATTRIBUTE_NAME = "phoneNumber";
    public static final String INDEX_KEY_ATTRIBUTE_NAME = "agentId";

    @NonNull private String phoneNumber;
    @NonNull private String status;
    @NonNull private String agentId;
    private Map<String, String> metadata;

    @DynamoDBHashKey(attributeName = PARTITION_KEY_ATTRIBUTE_NAME)
    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    @DynamoDBIndexHashKey(attributeName = INDEX_KEY_ATTRIBUTE_NAME, globalSecondaryIndexName = INDEX_KEY_ATTRIBUTE_NAME)
    public String getAgentId() {
        return this.agentId;
    }

    @DynamoDBAttribute
    public String getStatus() {
        return this.status;
    }

    @DynamoDBAttribute
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public Lead toLead() {
        return Lead.builder()
            .agentId(this.getAgentId())
            .phoneNumber(this.getPhoneNumber())
            .currentStatus(LeadStatus.valueOf(this.getStatus()))
            .metadata(this.getMetadata())
            .build();
    }
}
