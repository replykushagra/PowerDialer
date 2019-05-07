package com.power.dialer.compute;

import java.util.List;

import com.power.dialer.dao.PowerDialerDao;
import com.power.dialer.exception.AgentSessionTerminationException;
import com.power.dialer.exception.CallToLeadFailedException;
import com.power.dialer.exception.NoLeadsAvailableException;
import com.power.dialer.model.Agent;
import com.power.dialer.model.Agent.AgentStatus;
import com.power.dialer.model.Lead;
import com.power.dialer.model.Lead.LeadStatus;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PowerDialerImpl implements PowerDialer {

    private static final String DEFAULT_AGENT = "NONE";
    private final PowerDialerDao powerDialerDao;

    @Override
    public void onAgentLogin(final String agentId) {
        this.updateAgentStatus(agentId, AgentStatus.AVAILABLE);

        final String leadToDial = this.getLeadToDial(agentId);
        if (leadToDial == null || leadToDial.length() == 0) {
            throw new NoLeadsAvailableException(String.format("Could not find any leads for the agents %s", agentId)); 
        }

        // Generating another lead to dial for the agent
        this.getLeadToDial(agentId);

        try {
            this.makeCall(agentId, leadToDial);
        } catch (CallToLeadFailedException e) {
            this.onCallFailed(agentId, leadToDial);
        }
    }

    @Override
    public void onAgentLogout(final String agentId) {
        final Agent agent = this.powerDialerDao.getAgent(agentId);
        if (AgentStatus.ENGAGED.equals(agent.getAgentStatus())) {
            throw new AgentSessionTerminationException(String.format("Agent %s status %s. Could not terminate session", agentId, agent.getAgentStatus()));
        } else {
            this.updateAgentStatus(agentId, AgentStatus.OFF_DUTY);

            // Reset all the leads in Agent's queue
            List<Lead> leads = this.powerDialerDao.getAllLeads(agentId);
            if (leads != null) {
                leads.forEach(lead -> this.resetLead(lead.getPhoneNumber()));
            }
        }
    }

    @Override
    public void onCallStarted(final String agentId, final String phoneNumber) {
        final Agent agent = this.powerDialerDao.getAgent(agentId);

        if (AgentStatus.WAITING_TO_BE_ENGAGED.equals(agent.getAgentStatus())) {
            this.updateAgentStatus(agentId, AgentStatus.ENGAGED);
            this.updateLead(phoneNumber, LeadStatus.ENGAGED, agentId);
        }
    }

    @Override
    public void onCallFailed(final String agentId, final String phoneNumber) {
        this.makeNextCall(agentId);
        this.resetLead(phoneNumber);
    }

    @Override
    public void onCallEnded(final String agentId, final String phoneNumber) {
        this.makeNextCall(agentId);
        this.updateLeadStatus(phoneNumber, LeadStatus.COMPLETED);
    }

    private void updateLead(final String phoneNumber, final LeadStatus status, final String agentId) {
        final Lead lead = this.powerDialerDao.getLead(phoneNumber);
        this.powerDialerDao.updateLead(lead.toBuilder().currentStatus(status).agentId(agentId).build());
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
        this.powerDialerDao.updateAgent(agent.toBuilder().agentStatus(status).build());
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
                if (lead == null) {
                    this.updateAgentStatus(agentId, AgentStatus.AVAILABLE);
                    throw new NoLeadsAvailableException(String.format("Could not find any leads for the agents %s", agentId));
                } else {
                    this.makeCall(agentId, lead.getPhoneNumber());
                    break;
                }
            } catch (CallToLeadFailedException e) {
                if (++count == maxRetries) {
                    throw e;
                }
            }
        }
        // Update agent's queue with a new lead to ensure that there are 2 leads in an agent's queue at any given time
        final String newPhoneNumber = this.getLeadToDial(agentId);
        if (newPhoneNumber == null) {
            this.updateAgentStatus(agentId, AgentStatus.AVAILABLE);
        } else {
            this.updateLead(newPhoneNumber, LeadStatus.QUEUED, agentId);
        }
    }

    private String getLeadToDial(final String agentId) {

        String phoneNumber = this.powerDialerDao.getLeadPhoneNumberToDial();
        if (phoneNumber == null) {
            return null;
        }
        Lead lead = this.powerDialerDao.getLead(phoneNumber);
        int maxRetries = 5;
        int counter = 0;
        while (!lead.getCurrentStatus().equals(LeadStatus.AVAILABLE) && ++counter < maxRetries) {
            phoneNumber = this.powerDialerDao.getLeadPhoneNumberToDial();
            lead = this.powerDialerDao.getLead(phoneNumber);
        }
        if (phoneNumber == null) {
            throw new NoLeadsAvailableException(String.format("Could not find any leads for the agents %s", agentId));
        }
        // At this point the lead gets attached to an agent, so no other agent can be assigned to it
        this.updateLead(phoneNumber, LeadStatus.QUEUED, agentId);
        return phoneNumber;
    }

}
