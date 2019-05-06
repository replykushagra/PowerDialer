package com.power.dialer.compute;

import com.power.dialer.dao.PowerDialerDao;
import com.power.dialer.exception.AgentSessionTerminationException;
import com.power.dialer.exception.CallToLeadFailedException;
import com.power.dialer.model.Agent;
import com.power.dialer.model.Agent.AgentStatus;
import com.power.dialer.model.Lead;
import com.power.dialer.model.Lead.LeadStatus;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PowerDialerImpl implements PowerDialer {

    private static final String DEFAULT_AGENT = "NONE";
    private final PowerDialerDao powerDialerDao;

    public void onAgentLogin(final String agentId) {
        this.updateAgentStatus(agentId, AgentStatus.AVAILABLE);
        final String phoneNumber1 = this.powerDialerDao.getLeadPhoneNumberToDial();
        final String phoneNumber2 = this.powerDialerDao.getLeadPhoneNumberToDial();

        // May be add another status for the lead which has been put into call
        this.updateLead(phoneNumber1, LeadStatus.QUEUED, agentId);
        this.updateLead(phoneNumber2, LeadStatus.QUEUED, agentId);

        try {
            this.makeCall(agentId, phoneNumber1);
        } catch (CallToLeadFailedException e) {
            this.onCallFailed(agentId, phoneNumber1);
        }
    }

    public void onAgentLogout(final String agentId) {
        final Agent agent = this.powerDialerDao.getAgent(agentId);
        if (AgentStatus.ENGAGED.equals(agent.getStatus())) {
            throw new AgentSessionTerminationException(String.format("Agent %s status %s. Could not terminate session", agentId, agent.getStatus()));
        } else {
            this.updateAgentStatus(agentId, AgentStatus.OFF_DUTY);

            // Reset all the leads in Agent's queue
            Lead lead = this.powerDialerDao.getNextLead(agentId);
            while(lead != null) {
                this.resetLead(lead.getPhoneNumber());
                lead = this.powerDialerDao.getNextLead(agentId);
            }
        }
    }

    public void onCallStarted(final String agentId, final String phoneNumber) {
        final Agent agent = this.powerDialerDao.getAgent(agentId);

        if (AgentStatus.WAITING_TO_BE_ENGAGED.equals(agent.getStatus())) {
            this.updateAgentStatus(agentId, AgentStatus.ENGAGED);
            this.updateLead(phoneNumber, LeadStatus.ENGAGED, agentId);
        }
    }

    public void onCallFailed(final String agentId, final String phoneNumber) {
        this.updateAgentStatus(agentId, AgentStatus.AVAILABLE);
        this.resetLead(phoneNumber);
        this.makeNextCall(agentId);
    }

    public void onCallEnded(final String agentId, final String phoneNumber) {
        this.updateAgentStatus(agentId, AgentStatus.AVAILABLE);
        this.updateLeadStatus(phoneNumber, LeadStatus.COMPLETED);
        this.makeNextCall(agentId);
    }

    private void makeCall(final String agentId, final String phoneNumber) {
        this.powerDialerDao.dial(agentId, phoneNumber);
        this.updateLeadStatus(phoneNumber, LeadStatus.WAITING_TO_BE_ENGAGED);
        this.updateAgentStatus(agentId, AgentStatus.WAITING_TO_BE_ENGAGED);
    }

    private void updateLeadStatus(final String phoneNumber, final LeadStatus status) {
        final Lead lead = this.powerDialerDao.getLead(phoneNumber);
        this.powerDialerDao.updateLead(lead.toBuilder().currentStatus(status).build());
    }

    private void updateAgentStatus(final String agentId, final AgentStatus status) {
        final Agent agent = this.powerDialerDao.getAgent(agentId);
        this.powerDialerDao.updateAgent(agent.toBuilder().status(status).build());
    }

    private void updateLead(final String phoneNumber, final LeadStatus status, final String agentId) {
        final Lead lead = this.powerDialerDao.getLead(phoneNumber);
        this.powerDialerDao.updateLead(lead.toBuilder().currentStatus(status).agentId(agentId).build());
    }

    private void resetLead(final String phoneNumber) {
        final Lead lead = this.powerDialerDao.getLead(phoneNumber);
        this.powerDialerDao.updateLead(lead.toBuilder().currentStatus(LeadStatus.AVAILABLE).agentId(DEFAULT_AGENT).build());
    }

    private void makeNextCall(final String agentId) {
        int count = 0;
        int maxRetries = 5;
        while(true) {
            try {
                final Lead lead = this.powerDialerDao.getNextLead(agentId);
                this.makeCall(agentId, lead.getPhoneNumber());

                // Update agent's queue with a new lead to ensure that there are 2 leads in an agent's queue at any given time
                final String newPhoneNumber = this.powerDialerDao.getLeadPhoneNumberToDial();
                this.updateLead(newPhoneNumber, LeadStatus.QUEUED, agentId);
                return;
            } catch (CallToLeadFailedException e) {
                if (++count == maxRetries) {
                    throw e;
                }
            }
        }
    }
}
