/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.UUID;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.core.validation.ChainCheckCycle;
import org.oxymores.chronix.core.validation.ChainCheckEnds;
import org.sql2o.Connection;

@ChainCheckEnds
@ChainCheckCycle
public class Chain extends ActiveNodeBase
{
    private static final long serialVersionUID = -5369294333404575011L;

    @NotNull
    @Size(min = 2, max = 255, message = "a chain must have at least two states")
    @Valid
    protected ArrayList<State> states;

    @NotNull
    @Valid
    protected ArrayList<Transition> transitions;

    public Chain()
    {
        super();
        states = new ArrayList<>();
        transitions = new ArrayList<>();
    }

    public void addState(State state)
    {
        if (!this.states.contains(state))
        {
            this.states.add(state);
            state.chain = this;
        }
    }

    public ArrayList<State> getStates()
    {
        return states;
    }

    public State getState(UUID id)
    {
        for (State s : this.states)
        {
            if (s.getId().equals(id))
            {
                return s;
            }
        }
        return null;
    }

    public void addTransition(Transition tr)
    {
        if (!this.transitions.contains(tr))
        {
            this.transitions.add(tr);
            tr.chain = this;
        }
    }

    public ArrayList<Transition> getTransitions()
    {
        return transitions;
    }

    public State getStartState()
    {
        for (State s : states)
        {
            if (s.represents instanceof ChainStart)
            {
                return s;
            }
        }
        return null;
    }

    public State getEndState()
    {
        for (State s : states)
        {
            if (s.represents instanceof ChainEnd)
            {
                return s;
            }
        }
        return null;
    }

    public boolean isPlan()
    {
        return this.getStartState() == null;
    }

    // ///////////////////////////////////////////////////////////////////
    // Run methods
    @Override
    public void internalRun(Connection conn, ChronixContext ctx, PipelineJob pj, MessageProducer jmsProducer, Session jmsSession)
    {
        // Create a new run for the chain.
        pj.setBeganRunningAt(new DateTime());
        State s = this.getStartState();
        s.runInsidePlan(conn, jmsProducer, jmsSession, pj.getId(), null);
    }

    @Override
    public boolean hasInternalPayload()
    {
        return true;
    }

    @Override
    public boolean visibleInHistory()
    {
        return true;
    }

    // Run methods
    // ///////////////////////////////////////////////////////////////////
}
