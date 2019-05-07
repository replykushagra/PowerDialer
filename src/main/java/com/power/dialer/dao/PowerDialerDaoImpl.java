package com.power.dialer.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    public final static String DEFAULT_AGENT = "NONE";
    final static DynamoDBMapperConfig READ_CONFIG = DynamoDBMapperConfig.ConsistentReads.CONSISTENT.config();
    final static DynamoDBMapperConfig WRITE_CONFIG = DynamoDBMapperConfig.SaveBehavior.UPDATE.config();

    @NonNull private final DynamoDBMapper dynamoDBMapper;

    @Override
    public Agent getAgent(final String agentId) {
        final AgentRecord agentRecord = this.makeCallToDynamoDB(
            String.format("Getting the agent record for %s", agentId),
            () -> this.dynamoDBMapper.load(AgentRecord.class, agentId, READ_CONFIG));

        return agentRecord.toAgent();
    }

    @Override
    public Lead getLead(final String phoneNumber) {
        final LeadRecord leadRecord = this.makeCallToDynamoDB(
            String.format("Getting the lead record for %s", phoneNumber),
            () -> this.dynamoDBMapper.load(LeadRecord.class, phoneNumber, READ_CONFIG));

        return leadRecord.toLead();
    }

    @Override
    public Lead getNextLead(final String agentId) {
        final Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":agentId",  new AttributeValue().withS(agentId));

        final DynamoDBQueryExpression<LeadRecord> queryExpression = new DynamoDBQueryExpression<LeadRecord>()
            .withIndexName(LeadRecord.INDEX_KEY_ATTRIBUTE_NAME)
            .withKeyConditionExpression("agentId = :agentId")
            .withConsistentRead(false)
            .withExpressionAttributeValues(eav);

        final List<LeadRecord> records =  this.dynamoDBMapper.query(LeadRecord.class, queryExpression);
        Optional<Lead> optionalLead =  records.stream()
            .filter(record -> LeadStatus.QUEUED.toString().equals(record.getLeadStatus()))
            .map(LeadRecord::toLead)
            .findFirst();
        if (optionalLead.isPresent()) {
            return optionalLead.get();
        }
        return null;
    }

    @Override
    public void updateLead(final Lead lead) {
        this.makeCallToDynamoDB(String.format("Updating lead %s", lead.getPhoneNumber()),
            () -> {
                this.dynamoDBMapper.save(lead.toLeadRecord(), WRITE_CONFIG);
                return null;
            }
        );
    }

    @Override
    public void updateAgent(final Agent agent) { 
        this.makeCallToDynamoDB(String.format("Updating agent %s", agent.getAgentId()),
            () -> {
                this.dynamoDBMapper.save(agent.toRecord(), WRITE_CONFIG);
                return null;
            }
        );
    }

    @Override
    public void dial(final String agentId, final String phoneNumber) {
        // Not implemented
        return;
    }

    @Override
    public List<Lead> getAllLeads(final String agentId) {
        // Dummy implementation to make the test work
        final Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":agentId",  new AttributeValue().withS(agentId));

        final DynamoDBQueryExpression<LeadRecord> queryExpression = new DynamoDBQueryExpression<LeadRecord>()
            .withIndexName(LeadRecord.INDEX_KEY_ATTRIBUTE_NAME)
            .withKeyConditionExpression("agentId = :agentId")
            .withConsistentRead(false)
            .withExpressionAttributeValues(eav);

        final List<LeadRecord> records =  this.dynamoDBMapper.query(LeadRecord.class, queryExpression);
        // Ordering the results by phone number to make the unit tests deterministic
        return records.stream()
            .map(LeadRecord::toLead)
            .filter(lead -> lead.getCurrentStatus().equals(LeadStatus.QUEUED) ||
                  lead.getCurrentStatus().equals(LeadStatus.WAITING_TO_BE_ENGAGED))
            .collect(Collectors.toList());
    }

    @Override
    public String getLeadPhoneNumberToDial() {
        // Dummy implementation to make the test work
        final Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":agentId",  new AttributeValue().withS(DEFAULT_AGENT));
        eav.put(":leadStatus", new AttributeValue().withS(LeadStatus.AVAILABLE.toString()));

        final DynamoDBQueryExpression<LeadRecord> queryExpression = new DynamoDBQueryExpression<LeadRecord>()
            .withIndexName(LeadRecord.INDEX_KEY_ATTRIBUTE_NAME)
            .withKeyConditionExpression("agentId = :agentId and  leadStatus = :leadStatus")
            .withConsistentRead(false)
            .withExpressionAttributeValues(eav);

        final List<LeadRecord> records =  this.dynamoDBMapper.query(LeadRecord.class, queryExpression);
        // Ordering the results by phone number to make the unit tests deterministic
        Optional<LeadRecord> optionalLead =  records.stream()
            .sorted((o1, o2) -> o1.getPhoneNumber().compareTo(o2.getPhoneNumber()))
            .findFirst();
        if (optionalLead.isPresent()) {
            return optionalLead.get().getPhoneNumber();
        }
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
