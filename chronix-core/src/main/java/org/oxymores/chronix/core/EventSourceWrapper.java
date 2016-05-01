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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.FrameworkUtil;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source.OptionInvisible;
import org.oxymores.chronix.api.source.OptionOr;
import org.oxymores.chronix.api.source.RunModeDisabled;
import org.oxymores.chronix.api.source.RunModeExternalyTriggered;
import org.oxymores.chronix.api.source.RunModeForced;
import org.oxymores.chronix.api.source.RunModeTriggered;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;

public class EventSourceWrapper implements Serializable
{
    private static final long serialVersionUID = 2317281646089939267L;

    /////////////////////////////
    // Identity

    private UUID id;
    private String name;
    private String description;

    /////////////////////////////
    // Hydrated parameters

    private Map<String, ParameterHolder> fields = new HashMap<>(10);
    private List<ParameterHolder> additionalParameters = new ArrayList<>();

    /////////////////////////////
    // Sub elements if any

    protected List<State> states = null;
    protected List<DTOTransition> transitions = null;

    /////////////////////////////
    // Plugin identification

    /**
     * The unique key identifying the service responsible for this source
     */
    protected String behaviourClassName;

    /**
     * A simple indication - only used when a plugin is missing and we need its name to help the user.
     **/
    private String pluginName;

    /**
     * The provider associated to the event source. Not serialised ever (and likely not serialisable anyway)
     */
    private transient EventSourceProvider provider;

    /////////////////////////////
    // Engine misc

    /**
     * Whether the source should run enabled or disabled.
     */
    private boolean enabled = true;

    /**
     * True if contains other sources
     */
    private boolean container = false;

    public EventSourceWrapper(DTOEventSource source, ChronixContextMeta ctx, Application a)
    {
        super();

        this.behaviourClassName = source.getBehaviourClassName();
        this.provider = ctx.getSourceProvider(this.behaviourClassName);
        this.pluginName = FrameworkUtil.getBundle(this.provider.getClass()).getBundleContext().getBundle().getSymbolicName();

        this.name = source.getName();
        this.description = source.getDescription();
        this.id = source.getId();
        this.container = source instanceof DTOEventSourceContainer;
        if (this.container)
        {
            this.transitions = new ArrayList<>(((DTOEventSourceContainer) source).getContainedTransitions());
            this.states = new ArrayList<>(((DTOEventSourceContainer) source).getContainedStates().size());
            for (DTOState s : ((DTOEventSourceContainer) source).getContainedStates())
            {
                this.states.add(dto2state(a, s, this));
            }
        }

        for (DTOParameter prm : source.getAdditionalParameters())
        {
            this.additionalParameters.add(new ParameterHolder(prm, a, ctx));
        }

        for (DTOParameter prm : source.getFields())
        {
            this.fields.put(prm.getKey(), new ParameterHolder(prm, a, ctx));
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", name, pluginName);
    }

    private Object readResolve()
    {
        if (fields == null)
        {
            fields = new HashMap<>();
        }
        if (additionalParameters == null)
        {
            additionalParameters = new ArrayList<>();
        }
        return this;
    }

    public boolean isProvidedBy(Class<? extends EventSourceProvider> prv)
    {
        return prv.isInstance(this.provider);
    }

    public void setProvider(EventSourceProvider prv)
    {
        this.provider = prv;
    }

    ///////////////////////////////////////////////////////////////////////////
    // DTO

    public DTOEventSource getDTO()
    {
        if (this.isContainer())
        {
            DTOEventSourceContainer res = new DTOEventSourceContainer(provider, name, description, id);
            return res;
        }
        else
        {
            DTOEventSource res = new DTOEventSource(provider, name, description, id);
            return res;
        }
    }

    // DTO
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // stupid get/set

    public String getName()
    {
        return this.name;
    }

    public UUID getId()
    {
        return this.id;
    }

    public String getSourceTypeName()
    {
        return this.provider.getName();
    }

    public String getPluginSymbolicName()
    {
        return this.pluginName;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getPluginClassName()
    {
        return this.behaviourClassName;
    }

    // stupid get/set
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Container

    public boolean isContainer()
    {
        return this.container;
    }

    public List<State> getContainedStates()
    {
        if (!this.isContainer())
        {
            throw new ChronixPlanStorageException("a non-container event source has no states");
        }
        return this.states;
    }

    private List<DTOTransition> getContainedTransitions()
    {
        return this.transitions;
    }

    private State dto2state(Application a, DTOState d, EventSourceWrapper parent)
    {
        List<DTOTransition> trFromState = new ArrayList<>(), trToState = new ArrayList<>();
        for (DTOTransition tr : parent.getContainedTransitions())
        {
            if (tr.getFrom().equals(d.getId()))
            {
                trFromState.add(tr);
            }
            if (tr.getTo().equals(d.getId()))
            {
                trToState.add(tr);
            }
        }

        return new State(a, d, parent, trFromState, trToState);
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Parameter handling

    public List<ParameterHolder> getAdditionalParameters()
    {
        return this.additionalParameters;
    }

    public Collection<ParameterHolder> getFields()
    {
        return this.fields.values();
    }

    public List<ParameterHolder> getAllParameters()
    {
        List<ParameterHolder> res = new ArrayList<>(this.additionalParameters);
        res.addAll(this.fields.values());
        return res;
    }

    public List<ParameterHolder> getSubParametersOfType(String serviceClassName)
    {
        List<ParameterHolder> res = new ArrayList<>();
        for (ParameterHolder ph : this.getAllParameters())
        {
            res.addAll(ph.getSubParametersOfType(serviceClassName));
        }
        return res;
    }

    /**
     * null if not found.
     * 
     * @param key
     * @return
     */
    public ParameterHolder getField(UUID key)
    {
        for (ParameterHolder ph : this.fields.values())
        {
            if (ph.getParameterId().equals(key))
            {
                return ph;
            }
        }
        return null;
    }

    /**
     * null if not found.
     * 
     * @param key
     * @return
     */
    public ParameterHolder getAdditionalParameter(UUID key)
    {
        for (ParameterHolder ph : this.additionalParameters)
        {
            if (ph.getParameterId().equals(key))
            {
                return ph;
            }
        }
        return null;
    }

    // Parameter handling
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis

    public boolean isTransitionPossible(DTOTransition tr, Event evt)
    {
        return this.provider.isTransitionPossible(this.getDTO(), tr, evt);
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
        EventSourceRunResult esrr = this.checkEngineTriggered().run(this.getDTO(), cb, jd);
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
            return new RunResult(jd, ((RunModeForced) this.provider).runForceOk(this.getDTO(), cb, jd));
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
            return new RunResult(jd, ((RunModeDisabled) this.provider).runDisabled(this.getDTO(), cb, jd));
        }
        RunResult res = new RunResult(jd);
        res.end = jd.getVirtualTimeStart();
        res.logStart = "disabled";
        res.returnCode = 0;
        return res;
    }

    public RunModeTriggered checkEngineTriggered()
    {
        if (!this.isEngineTriggered())
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
        return this.provider instanceof OptionInvisible;
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
        return this.provider instanceof OptionOr;
    }

    //
    ///////////////////////////////////////////////////////////////////////////
}
