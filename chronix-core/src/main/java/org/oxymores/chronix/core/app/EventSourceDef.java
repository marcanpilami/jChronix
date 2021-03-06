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
package org.oxymores.chronix.core.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.FrameworkUtil;
import org.oxymores.chronix.api.exception.ChronixPluginException;
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
import org.oxymores.chronix.api.source.RunModeExternallyTriggered;
import org.oxymores.chronix.api.source.RunModeForced;
import org.oxymores.chronix.api.source.RunModeTriggered;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;
import org.oxymores.chronix.engine.data.RunResult;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;

public class EventSourceDef implements Serializable
{
    private static final long serialVersionUID = 2317281646089939267L;

    /////////////////////////////
    // Identity

    private UUID id;
    private String name;
    private String description;

    /////////////////////////////
    // Hydrated parameters

    private Map<String, ParameterDef> fields = new HashMap<>(10);
    private List<ParameterDef> additionalParameters = new ArrayList<>();

    /////////////////////////////
    // Sub elements if any

    private List<State> states = null;
    private List<DTOTransition> transitions = null;

    /////////////////////////////
    // Plugin identification

    /**
     * The unique key identifying the service responsible for this source
     */
    private String behaviourClassName;

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

    public EventSourceDef(DTOEventSource source, ChronixContextMeta ctx, Application a)
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
            this.additionalParameters.add(new ParameterDef(prm, a, ctx));
        }

        for (DTOParameter prm : source.getFields())
        {
            this.fields.put(prm.getKey(), new ParameterDef(prm, a, ctx));
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
            DTOEventSourceContainer res = new DTOEventSourceContainer(provider, null, name, description, id);
            for (State s : this.states)
            {
                res.addState(s.dto);
            }
            for (DTOTransition t : this.transitions)
            {
                res.addTransition(t);
            }

            return res;
        }
        else
        {
            DTOEventSource res = new DTOEventSource(provider, null, name, description, id);
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

    private State dto2state(Application a, DTOState d, EventSourceDef parent)
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

    public List<ParameterDef> getAdditionalParameters()
    {
        return this.additionalParameters;
    }

    public Collection<ParameterDef> getFields()
    {
        return this.fields.values();
    }

    public List<ParameterDef> getAllParameters()
    {
        List<ParameterDef> res = new ArrayList<>(this.additionalParameters);
        res.addAll(this.fields.values());
        return res;
    }

    public List<ParameterDef> getSubParametersOfType(String serviceClassName)
    {
        List<ParameterDef> res = new ArrayList<>();
        for (ParameterDef ph : this.getAllParameters())
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
    public ParameterDef getField(UUID key)
    {
        for (ParameterDef ph : this.fields.values())
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
    public ParameterDef getAdditionalParameter(UUID key)
    {
        for (ParameterDef ph : this.additionalParameters)
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
        if (this.isEngineTriggered())
        {
            EventSourceRunResult esrr = ((RunModeTriggered) this.provider).run(this.getDTO(), cb, jd);
            if (esrr != null)
            {
                return new RunResult(jd, esrr);
            }
        }
        if (this.isExternallyTriggered())
        {
            EventSourceRunResult esrr = ((RunModeExternallyTriggered) this.provider).run(this.getDTO(), jd);
            if (esrr == null)
            {
                throw new ChronixPluginException("an external plugin has returned a null result. This is forbidden");
            }
            esrr.overloadedScopeId = new UUID(0, 1); // External source states are ALWAYS on the global scope.
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
        return this.provider instanceof RunModeExternallyTriggered;
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

    public boolean isExternallyTriggered()
    {
        return this.provider instanceof RunModeExternallyTriggered;
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
