package com.power.dialer.model;

import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.power.dialer.model.Lead.LeadStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Builder(toBuilder = true)
@AllArgsConstructor
@Data
@NoArgsConstructor
@DynamoDBTable(tableName = "lead")
public class LeadRecord {

    public static final String PARTITION_KEY_ATTRIBUTE_NAME = "phoneNumber";
    public static final String INDEX_KEY_ATTRIBUTE_NAME = "agentId-leadStatus";

    @NonNull private String phoneNumber;
    @NonNull private String leadStatus;
    @NonNull private String agentId;
    private Map<String, String> metadata;

    @DynamoDBHashKey(attributeName = PARTITION_KEY_ATTRIBUTE_NAME)
    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = INDEX_KEY_ATTRIBUTE_NAME)
    public String getAgentId() {
        return this.agentId;
    }

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = INDEX_KEY_ATTRIBUTE_NAME)
    public String getLeadStatus() {
        return this.leadStatus;
    }

    @DynamoDBAttribute
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public Lead toLead() {
        return Lead.builder()
            .agentId(this.getAgentId())
            .phoneNumber(this.getPhoneNumber())
            .currentStatus(LeadStatus.valueOf(this.getLeadStatus()))
            .metadata(this.getMetadata())
            .build();
    }
}
