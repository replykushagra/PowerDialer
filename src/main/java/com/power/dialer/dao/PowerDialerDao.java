package com.power.dialer.dao;

import java.util.List;

import com.power.dialer.model.Agent;
import com.power.dialer.model.Lead;

public interface PowerDialerDao {

    /**
     * Returns the agent given an agentId
     * @param agentId Agent id
     * @return Agent
     */
    Agent getAgent(final String agentId);

    /**
     * Returns the lead given a phone number
     * @param phoneNumber phone number
     * @return Lead
     */
    Lead getLead(final String phoneNumber);

    /**
     * Return the next lead to be dialed for the agent
     * @param agentId agent id
     * @return next Lead
     */
    Lead getNextLead(final String agentId);

    /**
     * Return all the leads in the Agent's queue
     * @param agentId agent id
     * @return all leads in Agent's queue
     */
    List<Lead> getAllLeads(final String agentId);

    /**
     * Returns the next phone number to dial
     * @return phone number to dial
     */
    String getLeadPhoneNumberToDial();

    /**
     * Updates the Lead
     * @param lead Lead
     */
    void updateLead(final Lead lead);

    /**
     * Updates the agent
     * @param agent Agent
     */
    void updateAgent(final Agent agent);

    /**
     * Attempts to establish the communication between agent and lead
     * @param agentId agent id
     * @param phoneNumber lead phone number
     */
    void dial(final String agentId, final String phoneNumber);
}
