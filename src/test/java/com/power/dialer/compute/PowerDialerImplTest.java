package com.power.dialer.compute;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.power.dialer.dao.PowerDialerDao;
import com.power.dialer.dao.PowerDialerDaoImpl;
import com.power.dialer.exception.AgentSessionTerminationException;
import com.power.dialer.exception.NoLeadsAvailableException;
import com.power.dialer.model.Agent.AgentStatus;
import com.power.dialer.model.AgentRecord;
import com.power.dialer.model.Lead;
import com.power.dialer.model.LeadRecord;
import com.power.dialer.model.Lead.LeadStatus;

public class PowerDialerImplTest {

    private static final String LEAD_PHONE_NUMBER1 = "1";
    private static final String LEAD_PHONE_NUMBER2 = "2";
    private static final String LEAD_PHONE_NUMBER3 = "3";
    private static final String LEAD_PHONE_NUMBER4 = "4";
    private static final String AGENT1 = "agent1";
    private static final String AGENT2 = "agent2";
    private static final String AGENT3 = "agent3";
    private static final String DEFAULT_AGENT = "NONE";
    
    private PowerDialerImpl underTest;
    private PowerDialerDao powerDialerDao;
    private DynamoDBMapper dynamoDbMapper = null;
    private AmazonDynamoDB dynamoDb = null;

    @Before
    public void init() {
        this.dynamoDb = DynamoDBEmbedded.create().amazonDynamoDB();
        this.dynamoDbMapper = new DynamoDBMapper(this.dynamoDb);
        this.powerDialerDao = new PowerDialerDaoImpl(this.dynamoDbMapper);
        this.underTest = new PowerDialerImpl(powerDialerDao);
        this.createTables();
        this.createAgentAndLeads();
    }

    @After
    public void tearDown() {
        final DeleteTableRequest deleteLeadTable = new DeleteTableRequest();
        deleteLeadTable.setTableName("lead");
        this.dynamoDb.deleteTable(deleteLeadTable);
        final DeleteTableRequest deleteAgentTable = new DeleteTableRequest();
        deleteAgentTable.setTableName("agent");
        this.dynamoDb.deleteTable(deleteAgentTable);
    }

    @Test
    public void testOnAgentLogin_success() {
        this.underTest.onAgentLogin(AGENT1);
        final Lead agent1LeadDialed = this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1);
        final Lead agent1LeadQueued = this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2);

        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, agent1LeadDialed.getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, agent1LeadQueued.getCurrentStatus());

        this.underTest.onAgentLogin(AGENT2);
        final Lead agent2LeadDialed = this.powerDialerDao.getLead(LEAD_PHONE_NUMBER3);
        final Lead agent2LeadQueued = this.powerDialerDao.getLead(LEAD_PHONE_NUMBER4);

        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, agent2LeadDialed.getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, agent2LeadQueued.getCurrentStatus());
    }

    @Test(expected = NoLeadsAvailableException.class)
    public void testOnAgentLogin_withNoLeadsAvailable_throwsNoLeadsAvailableException() {
        this.underTest.onAgentLogin(AGENT1);
        this.underTest.onAgentLogin(AGENT2);
        // All the available leads have already been assigned to the above agents
        this.underTest.onAgentLogin(AGENT3);        
    }

    @Test
    public void testOnAgentLogout_success() {
        // Assigning 2 leads to agent1
        this.underTest.onAgentLogin(AGENT1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());

        this.underTest.onAgentLogout(AGENT1);
        assertEquals(LeadStatus.AVAILABLE, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
        assertEquals(LeadStatus.AVAILABLE, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());
        assertEquals(AgentStatus.OFF_DUTY, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
    }

    @Test(expected = AgentSessionTerminationException.class)
    public void testOnAgentLogout_whenAgentEngagedOnACall_throwsAgentSessionTerminationException() {
        // Assigning 2 leads to agent1
        this.underTest.onAgentLogin(AGENT1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());

        this.underTest.onCallStarted(AGENT1, LEAD_PHONE_NUMBER1);
        try {
            this.underTest.onAgentLogout(AGENT1);
        } finally {
            assertEquals(AgentStatus.ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        }
    }

    @Test
    public void testOnCallStarted_leadAndAgentStatusUpdatesToEngaged() {
        this.underTest.onAgentLogin(AGENT1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());

        this.underTest.onCallStarted(AGENT1, LEAD_PHONE_NUMBER1);
        assertEquals(AgentStatus.ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
    }

    @Test
    public void testOnCallFailed_leadStatusUpdatesToAvailable_agentStatusUpdatesToWaitingToBeEnagedWithNextLead() {
        this.underTest.onAgentLogin(AGENT1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());

        this.underTest.onCallFailed(AGENT1, LEAD_PHONE_NUMBER1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());
        assertEquals(LeadStatus.AVAILABLE, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
    }

    @Test
    public void testOnCallEnded_leadStatusUpdatesToWaitingToBeEngagedWithNextLead_currentLeadMovesToCompleted() {
        this.underTest.onAgentLogin(AGENT1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
        assertEquals(LeadStatus.QUEUED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());

        this.underTest.onCallEnded(AGENT1, LEAD_PHONE_NUMBER1);
        assertEquals(AgentStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getAgent(AGENT1).getAgentStatus());
        assertEquals(LeadStatus.WAITING_TO_BE_ENGAGED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER2).getCurrentStatus());
        assertEquals(LeadStatus.COMPLETED, this.powerDialerDao.getLead(LEAD_PHONE_NUMBER1).getCurrentStatus());
    }

    private void createAgentAndLeads() {
        this.dynamoDbMapper.save(AgentRecord.builder().agentId(AGENT1).agentStatus(AgentStatus.OFF_DUTY.toString()).build());
        this.dynamoDbMapper.save(AgentRecord.builder().agentId(AGENT2).agentStatus(AgentStatus.OFF_DUTY.toString()).build());
        this.dynamoDbMapper.save(AgentRecord.builder().agentId(AGENT3).agentStatus(AgentStatus.OFF_DUTY.toString()).build());

        this.dynamoDbMapper.save(LeadRecord.builder().agentId(DEFAULT_AGENT).phoneNumber(LEAD_PHONE_NUMBER1).leadStatus(LeadStatus.AVAILABLE.toString()).build());
        this.dynamoDbMapper.save(LeadRecord.builder().agentId(DEFAULT_AGENT).phoneNumber(LEAD_PHONE_NUMBER2).leadStatus(LeadStatus.AVAILABLE.toString()).build());
        this.dynamoDbMapper.save(LeadRecord.builder().agentId(DEFAULT_AGENT).phoneNumber(LEAD_PHONE_NUMBER3).leadStatus(LeadStatus.AVAILABLE.toString()).build());
        this.dynamoDbMapper.save(LeadRecord.builder().agentId(DEFAULT_AGENT).phoneNumber(LEAD_PHONE_NUMBER4).leadStatus(LeadStatus.AVAILABLE.toString()).build());
    }

    private void createTables() {
        final ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput(1L, 1L);

        final CreateTableRequest createAgentTableRequest =
            this.dynamoDbMapper.generateCreateTableRequest(AgentRecord.class);
        final CreateTableRequest createLeadTableRequest =
            this.dynamoDbMapper.generateCreateTableRequest(LeadRecord.class);

        createAgentTableRequest.setProvisionedThroughput(provisionedThroughput);
        createLeadTableRequest.setProvisionedThroughput(provisionedThroughput);
        createLeadTableRequest.getGlobalSecondaryIndexes().forEach(index -> index.setProvisionedThroughput(provisionedThroughput));
        createLeadTableRequest.getGlobalSecondaryIndexes().forEach(index -> index.setProjection(new Projection().withProjectionType(
            ProjectionType.ALL)));

        this.dynamoDb.createTable(createAgentTableRequest);
        this.dynamoDb.createTable(createLeadTableRequest);
    }
}
