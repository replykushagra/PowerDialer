package com.power.dialer.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.power.dialer.exception.PowerDialerDependencyException;
import com.power.dialer.model.Agent;
import com.power.dialer.model.AgentRecord;
import com.power.dialer.model.Lead;
import com.power.dialer.model.Lead.LeadStatus;
import com.power.dialer.model.LeadRecord;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class PowerDialerDaoImpl implements PowerDialerDao {

    final static DynamoDBMapperConfig READ_CONFIG = DynamoDBMapperConfig.ConsistentReads.CONSISTENT.config();
    final static DynamoDBMapperConfig WRITE_CONFIG = DynamoDBMapperConfig.SaveBehavior.UPDATE.config();

    @NonNull private final DynamoDBMapper dynamoDBMapper;

    @Override
    public Agent getAgent(String agentId) {
        final AgentRecord agentRecord = this.makeCallToDynamoDB(
            String.format("Getting the agent record for %s", agentId),
            () -> this.dynamoDBMapper.load(AgentRecord.class, agentId, READ_CONFIG));

        return agentRecord.toAgent();
    }

    @Override
    public Lead getLead(String phoneNumber) {
        final LeadRecord leadRecord = this.makeCallToDynamoDB(
            String.format("Getting the lead record for %s", phoneNumber),
            () -> this.dynamoDBMapper.load(LeadRecord.class, phoneNumber, READ_CONFIG));

        return leadRecord.toLead();
    }

    @Override
    public Lead getNextLead(String agentId) {
        final Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":agentId",  new AttributeValue().withS(agentId));

        final DynamoDBQueryExpression<LeadRecord> queryExpression = new DynamoDBQueryExpression<LeadRecord>()
            .withIndexName(LeadRecord.INDEX_KEY_ATTRIBUTE_NAME)
            .withKeyConditionExpression("agentId = :agentId")
            .withExpressionAttributeValues(eav);

        final List<LeadRecord> records =  this.dynamoDBMapper.query(LeadRecord.class, queryExpression);
        return records.stream()
            .filter(record -> LeadStatus.WAITING_TO_BE_ENGAGED.toString().equals(record.getStatus()))
            .map(LeadRecord::toLead)
            .findFirst()
            .get();
    }

    @Override
    public void updateLead(Lead lead) {
        this.makeCallToDynamoDB(String.format("Updating lead %s", lead.getPhoneNumber()),
            () -> {
                this.dynamoDBMapper.save(lead.toLeadRecord(), WRITE_CONFIG);
                return null;
            }
        );
    }

    @Override
    public void updateAgent(Agent agent) { 
        this.makeCallToDynamoDB(String.format("Updating agent %s", agent.getAgentId()),
            () -> {
                this.dynamoDBMapper.save(agent.toRecord(), WRITE_CONFIG);
                return null;
            }
        );
    }

    @Override
    public void dial(String agentId, String phoneNumber) {
        // Not implemented
        return;
    }

    @Override
    public String getLeadPhoneNumberToDial() {
        // Not implemented
        return null;
    }

    private <T> T makeCallToDynamoDB(final String eventMessage, final Supplier<T> dynamoLambda) {
        try {
            final T result = dynamoLambda.get();
            return result;
        } catch (final ConditionalCheckFailedException ccfe) {
            final String errorMsg = String.format("Condition check failed when: %s", eventMessage);
            throw new PowerDialerDependencyException(errorMsg, ccfe);
        } catch (final AmazonServiceException ase) {
            final String errorMsg = String.format("An AWS error occured when: %s", eventMessage);
            throw new PowerDialerDependencyException(errorMsg, ase);
        } catch (final Exception e) {
            final String errorMsg = String.format("An unexpected error occured when: %s", eventMessage);
            throw new PowerDialerDependencyException(errorMsg, e);
        }
    }

}
