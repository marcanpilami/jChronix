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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.oxymores.chronix.core.context.Application2;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.EventSourceOptionInvisible;
import org.oxymores.chronix.core.source.api.EventSourceSelfTriggered;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;
import org.oxymores.chronix.engine.modularity.runner.RunResult;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class EventSourceWrapper implements Serializable
{
    private static final long serialVersionUID = 2317281646089939267L;

    @NotNull
    protected ArrayList<Parameter> parameters;

    private Application2 application;

    // A simple indication - only used when a plugin is missing and we need its name to help the user.
    private String pluginName;

    // The real event source description. Must NOT be XML-serialised. Each plugin is responsible for its own serialisation.
    @XStreamOmitField
    private EventSource eventSource;

    private boolean enabled = true;

    public EventSourceWrapper(Application2 app, EventSource source, String pluginSymbolicName)
    {
        super();
        this.application = app;
        this.eventSource = source;
        this.pluginName = pluginSymbolicName;
        parameters = new ArrayList<>();
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", eventSource.getName(), pluginName);
    }

    ///////////////////////////////////////////////////////////////////////////
    // stupid get/set

    public String getName()
    {
        return this.eventSource.getName();
    }

    public UUID getId()
    {
        return this.eventSource.getId();
    }

    public String getSourceClass()
    {
        return this.eventSource.getClass().getCanonicalName();
    }

    public String getPluginSymbolicName()
    {
        return this.pluginName;
    }

    public EventSource getSource()
    {
        return this.eventSource;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    // stupid get/set
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Relationship traversing

    public List<State> getClientStates()
    {
        return this.application.getStatesClientOfSource(getId());
    }

    // Relationship traversing
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Parameter handling

    public ArrayList<Parameter> getParameters()
    {
        return this.parameters;
    }

    public void addParameter(Parameter parameter)
    {
        if (!parameters.contains(parameter))
        {
            parameters.add(parameter);
        }
    }

    public void addParameter(String key, String value, String description)
    {
        Parameter p = new Parameter();
        p.setDescription(description);
        p.setKey(key);
        p.setValue(value);
        addParameter(p);
    }

    // Parameter handling
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis

    public boolean isTransitionPossible(DTOTransition tr, Event evt)
    {
        return this.eventSource.isTransitionPossible(tr, evt);
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Methods called before and after run
    // Run - phase 1
    // Responsible for parameters resolution.
    // Default implementation resolves all parameters. Should usually be called
    // by overloads.

    public void prepareRun(PipelineJob pj, RunnerManager sender)
    {
        /*
         * for (Parameter p : this.parameters) { p.resolveValue(ctx, sender, pj); }
         */
    }

    public RunResult run(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult esrr = this.checkEngineTriggered().run(cb, jd);
        if (esrr != null)
        {
            return new RunResult(jd, esrr);
        }
        return null;
    }

    public RunResult forceOK(EngineCallback cb, JobDescription jd)
    {
        return new RunResult(jd, this.checkEngineTriggered().runForceOk(cb, jd));
    }

    public RunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return new RunResult(jd, this.checkEngineTriggered().runDisabled(cb, jd));
    }

    public EventSourceTriggered checkEngineTriggered()
    {
        if (this.eventSource == null || !this.isEngineTriggered())
        {
            throw new IllegalStateException("trying to trigger a source that cannot be run by the engine");
        }
        return (EventSourceTriggered) this.eventSource;
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Toggles

    public boolean isSelfTriggered()
    {
        return this.eventSource instanceof EventSourceSelfTriggered;
    }

    public boolean isHiddenFromHistory()
    {
        return this.eventSource instanceof EventSourceOptionInvisible;
    }

    public boolean isContainer()
    {
        return this.eventSource instanceof EventSourceContainer;
    }

    public boolean isEngineTriggered()
    {
        return this.eventSource instanceof EventSourceTriggered;
    }

    //
    ///////////////////////////////////////////////////////////////////////////
}
