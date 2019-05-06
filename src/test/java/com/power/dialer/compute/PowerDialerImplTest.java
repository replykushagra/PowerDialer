package com.power.dialer.compute;

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
import com.power.dialer.model.AgentRecord;
import com.power.dialer.model.LeadRecord;

public class PowerDialerImplTest {

    private PowerDialerImpl underTest;
    private PowerDialerDao powerDialerDao;
    private DynamoDBMapper dynamoDbMapper = null;
    private AmazonDynamoDB dynamoDb = null;

    @Before
    public void init() {
        //System.setProperty("sqlite4java.library.path", "native-libs");
        this.dynamoDb = DynamoDBEmbedded.create().amazonDynamoDB();
        this.dynamoDbMapper = new DynamoDBMapper(this.dynamoDb);
        this.powerDialerDao = new PowerDialerDaoImpl(this.dynamoDbMapper);
        this.underTest = new PowerDialerImpl(powerDialerDao);
        this.createTables();
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
    public void xy() {
    }

    public void createTables() {
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
