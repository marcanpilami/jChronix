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

import org.osgi.framework.FrameworkUtil;
import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceOptionInvisible;
import org.oxymores.chronix.api.source.EventSourceOptionOr;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source2.DTOEventSource;
import org.oxymores.chronix.api.source2.DTOEventSourceContainer;
import org.oxymores.chronix.api.source2.EventSourceProvider;
import org.oxymores.chronix.api.source2.RunModeDisabled;
import org.oxymores.chronix.api.source2.RunModeExternalyTriggered;
import org.oxymores.chronix.api.source2.RunModeForced;
import org.oxymores.chronix.api.source2.RunModeTriggered;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;

public class EventSourceWrapper implements Serializable
{
    private static final long serialVersionUID = 2317281646089939267L;

    /**
     * A simple indication - only used when a plugin is missing and we need its name to help the user.
     **/
    private String pluginName;

    /**
     * The real event source description.
     */
    private DTOEventSource eventSource;

    /**
     * The provider associated to the event source. Not serialised ever. Duplicate from the DTO - this way, the DTO does not expose the
     * provider.
     */
    private transient EventSourceProvider provider;

    /**
     * Whether the source should run enabled or disabled.
     */
    private boolean enabled = true;

    private List<ParameterHolder> parameters = new ArrayList<>();

    public EventSourceWrapper(DTOEventSource source, EventSourceProvider provider)
    {
        super();
        this.eventSource = source;
        this.provider = provider;
        this.eventSource.setBehaviour(provider);
        this.pluginName = FrameworkUtil.getBundle(provider.getClass()).getBundleContext().getBundle().getSymbolicName();
    }

    @Override
    public String toString()
    {
        if (eventSource != null)
        {
            return String.format("%s (%s)", eventSource.getName(), pluginName);
        }
        else
        {
            return String.format("Event source from plugin %s (notloaded yet)", this.pluginName);
        }
    }

    private Object readResolve()
    {
        if (parameters == null)
        {
            parameters = new ArrayList<>();
        }
        return this;
    }

    public boolean isInstanceOf(Class<? extends EventSourceProvider> prv)
    {
        return prv.isInstance(this.provider);
    }

    public void setProvider(EventSourceProvider prv)
    {
        this.eventSource.setBehaviour(prv);
        this.provider = prv;
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

    public DTOEventSource getSource()
    {
        return this.eventSource;
    }

    public void setSource(DTOEventSource s, String pluginName)
    {
        this.eventSource = s;
        this.pluginName = pluginName;
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
    // Parameter handling

    public List<ParameterHolder> getParameters()
    {
        return this.parameters;
    }

    public void addParameter(ParameterHolder parameter)
    {
        if (!parameters.contains(parameter))
        {
            parameters.add(parameter);
        }
    }

    public void addParameter(String key, String description, Parameter prm)
    {
        ParameterHolder p = new ParameterHolder();
        p.setDescription(description);
        p.setKey(key);
        p.setDto(prm);
        addParameter(p);
    }

    // Parameter handling
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis

    public boolean isTransitionPossible(DTOTransition tr, Event evt)
    {
        return this.provider.isTransitionPossible(this.eventSource, tr, evt);
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
        EventSourceRunResult esrr = this.checkEngineTriggered().run(this.eventSource, cb, jd);
        if (esrr != null)
        {
            return new RunResult(jd, esrr);
        }
        return null;
    }

    public RunResult forceOK(EngineCallback cb, JobDescription jd)
    {
        if (this.hasSpecificForceOkMethod())
        {
            return new RunResult(jd, ((RunModeForced) this.provider).runForceOk(this.eventSource, cb, jd));
        }
        RunResult res = new RunResult(jd);
        res.end = jd.getVirtualTimeStart();
        res.logStart = "forced OK";
        res.returnCode = 0;
        return res;
    }

    public RunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        if (this.hasSpecificDisabledMethod())
        {
            return new RunResult(jd, ((RunModeDisabled) this.provider).runDisabled(this.eventSource, cb, jd));
        }
        RunResult res = new RunResult(jd);
        res.end = jd.getVirtualTimeStart();
        res.logStart = "disabled";
        res.returnCode = 0;
        return res;
    }

    public RunModeTriggered checkEngineTriggered()
    {
        if (this.eventSource == null || !this.isEngineTriggered())
        {
            throw new IllegalStateException("trying to trigger a source that cannot be run by the engine");
        }
        return (RunModeTriggered) this.provider;
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Toggles

    public boolean isSelfTriggered()
    {
        return this.provider instanceof RunModeExternalyTriggered;
    }

    public boolean hasSpecificDisabledMethod()
    {
        return this.provider instanceof RunModeDisabled;
    }

    public boolean hasSpecificForceOkMethod()
    {
        return this.provider instanceof RunModeForced;
    }

    public boolean isHiddenFromHistory()
    {
        return this.provider instanceof EventSourceOptionInvisible;
    }

    public boolean isContainer()
    {
        return this.eventSource instanceof DTOEventSourceContainer;
    }

    public boolean isEngineTriggered()
    {
        return this.provider instanceof RunModeTriggered;
    }

    public boolean isAnd()
    {
        return !this.isOr();
    }

    public boolean isOr()
    {
        return this.provider instanceof EventSourceOptionOr;
    }

    //
    ///////////////////////////////////////////////////////////////////////////
}
