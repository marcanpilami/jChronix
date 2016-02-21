package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.EventSourceWrapper;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.exceptions.ChronixException;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application2 implements IMetaSource, Serializable
{
    private static final long serialVersionUID = -7565792688611748679L;
    private static final Logger log = LoggerFactory.getLogger(Application2.class);
    static final BundleContext osgiCtx = FrameworkUtil.getBundle(Application2.class).getBundleContext();

    static final int currentModelVersion = 1;
    static final int compatibleUpToBackwards = 0;

    @Min(0)
    private int modelVersion = currentModelVersion;
    private DateTime latestSave = DateTime.now();
    private List<ApplicationVersion> versions = new ArrayList<>();

    @NotNull
    private UUID id = UUID.randomUUID();
    @NotNull
    @Size(min = 1, max = 50)
    private String name;
    @NotNull
    @Size(min = 1, max = 255)
    private String description;

    @Valid
    private Map<UUID, PlaceGroup> groups = new HashMap<>();

    @Valid
    private Map<UUID, Token> tokens = new HashMap<>();

    @Valid
    protected Map<UUID, Calendar> calendars = new HashMap<>();

    // The sources
    private Map<UUID, EventSourceWrapper> sources = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    public Application2()
    {
        this.versions.add(new ApplicationVersion(0, "application creation"));
    }

    private Object readResolve()
    {
        if (sources == null)
        {
            sources = new HashMap<>();
        }
        if (groups == null)
        {
            groups = new HashMap<>();
        }
        if (tokens == null)
        {
            tokens = new HashMap<>();
        }
        if (calendars == null)
        {
            calendars = new HashMap<>();
        }
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner class helpers
    ///////////////////////////////////////////////////////////////////////////

    protected class ApplicationVersion implements Serializable
    {
        ApplicationVersion(int version, String commitComment)
        {
            this.version = version;
            this.versionComment = commitComment;
            this.created = DateTime.now();
        }

        private static final long serialVersionUID = 338399455626386055L;

        int version = 0;
        String versionComment = "";
        DateTime created;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Source handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSource getEventSource(UUID id)
    {
        if (!this.containsSource(id))
        {
            throw new ChronixException("non existing source");
        }
        return this.sources.get(id).getSource();
    }

    public EventSourceWrapper getEventSourceContainer(UUID id)
    {
        if (!this.containsSource(id))
        {
            throw new ChronixException("non existing source");
        }
        return this.sources.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T extends EventSource> List<T> getEventSources(Class<T> klass)
    {
        List<T> res = new ArrayList<>();
        for (EventSourceWrapper d : this.sources.values())
        {
            if (d != null && klass.isInstance(d.getSource()))
            {
                res.add((T) d.getSource());
            }
        }
        return res;
    }

    /**
     * Returns a copy of the event source list.
     */
    public List<EventSource> getEventSources()
    {
        List<EventSource> res = new ArrayList<>();
        for (EventSourceWrapper s : this.sources.values())
        {
            res.add(s.getSource());
        }
        return res;
    }

    @Override
    public <T extends EventSource & Serializable> void registerSource(T source, String pluginSymbolicName)
    {
        log.trace("Registering event source with ID " + source.getId() + " from plugin " + pluginSymbolicName + " of class "
                + source.getClass().getSimpleName() + " - " + this.toString());
        EventSourceWrapper esc = new EventSourceWrapper(this, source, pluginSymbolicName);
        sources.put(source.getId(), esc);
    }

    @Override
    public <T extends EventSource> void unregisterSource(T source)
    {
        if (this.sources.containsKey(source.getId()))
        {
            this.sources.remove(source.getId());
        }
    }

    public boolean containsSource(EventSource source)
    {
        return this.containsSource(source.getId());
    }

    public boolean containsSource(UUID id)
    {
        return this.sources.containsKey(id);
    }

    public Set<Class<? extends EventSource>> getAllEventSourceClasses()
    {
        Set<Class<? extends EventSource>> res = new HashSet<>();
        for (EventSourceWrapper es : this.sources.values())
        {
            res.add(es.getSource().getClass());
        }
        return res;
    }

    public List<EventSource> getEventSources(EventSourceProvider pr)
    {
        List<EventSource> res = new ArrayList<>();
        for (EventSourceWrapper s : this.sources.values())
        {
            if (s.getSource().getProvider().equals(pr.getClass()))
            {
                res.add(s.getSource());
            }
        }
        return res;
    }

    void waitForAllPlugins()
    {
        Set<String> plugins = new HashSet<>();
        for (EventSourceWrapper w : this.sources.values())
        {
            plugins.add(w.getPluginSymbolicName());
        }
        log.info("Application " + this.name + " has " + this.sources.size() + " sources coming from the following plugins: "
                + plugins.toString());

        ServiceTracker<EventSourceProvider, EventSourceProvider> tracker = new ServiceTracker<EventSourceProvider, EventSourceProvider>(
                FrameworkUtil.getBundle(Application2.class).getBundleContext(), EventSourceProvider.class, null);
        int maxWaitSec = 60;
        int waitedSec = 0;
        int waitStepSec = 1;

        try
        {
            tracker.open();
            nextplugin: for (String symbolicName : plugins)
            {
                while (true)
                {
                    ServiceReference<EventSourceProvider>[] refs = tracker.getServiceReferences();
                    if (refs != null)
                    {
                        for (ServiceReference<EventSourceProvider> ref : refs)
                        {
                            if (ref.getBundle().getSymbolicName().equals(symbolicName))
                            {
                                // Found
                                continue nextplugin;
                            }
                        }
                    }

                    // Not found - wait and try again
                    try
                    {
                        Thread.sleep(waitStepSec * 1000);
                        waitedSec += waitStepSec;
                        if (waitedSec > maxWaitSec)
                        {
                            throw new ChronixInitializationException(
                                    "Cannot load application " + this.name + " as it uses missing plugin " + symbolicName);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        // not an issue.
                    }
                }
            }

        }
        finally
        {
            tracker.close();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // VERSIONS
    ///////////////////////////////////////////////////////////////////////////

    public final void addVersion(String comment)
    {
        ApplicationVersion v = new ApplicationVersion(getVersion() + 1, comment);
        this.versions.add(v);
    }

    public int getVersion()
    {
        return this.versions.get(this.versions.size() - 1).version;
    }

    public String getCommitComment()
    {
        return this.versions.get(this.versions.size() - 1).versionComment;
    }

    ///////////////////////////////////////////////////////////////////////////
    // GET/SET
    ///////////////////////////////////////////////////////////////////////////

    public UUID getId()
    {
        return this.id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public DateTime getLatestSave()
    {
        return latestSave;
    }

    void setLatestSave(DateTime latestSave)
    {
        this.latestSave = latestSave;
    }

    ///////////////////////////////////////////////////////////////////////////
    // TOKENS
    ///////////////////////////////////////////////////////////////////////////

    public Token getToken(UUID id)
    {
        return this.tokens.get(id);
    }

    public void addToken(Token t)
    {
        if (!this.tokens.containsValue(t))
        {
            this.tokens.put(t.id, t);
            t.setApplication(this);
        }
    }

    public void removeToken(Token t)
    {
        this.tokens.remove(t.id);
        t.setApplication(null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // GROUPS
    ///////////////////////////////////////////////////////////////////////////

    public void addGroup(PlaceGroup o)
    {
        if (!this.groups.containsValue(o))
        {
            this.groups.put(o.id, o);
            o.setApplication(this);
        }
    }

    public void removeGroup(PlaceGroup o)
    {
        this.groups.remove(o.id);
        o.setApplication(null);
    }

    public Map<UUID, PlaceGroup> getGroups()
    {
        HashMap<UUID, PlaceGroup> res = new HashMap<>();
        res.putAll(this.groups);
        return res;
    }

    public List<PlaceGroup> getGroupsList()
    {
        return new ArrayList<>(this.groups.values());
    }

    public PlaceGroup getGroup(String name)
    {
        PlaceGroup res = null;
        for (PlaceGroup pg : this.groups.values())
        {
            if (pg.getName().equals(name))
            {
                return pg;
            }
        }
        return res;
    }

    public PlaceGroup getGroup(UUID id)
    {
        return this.groups.get(id);
    }

    ///////////////////////////////////////////////////////////////////////////
    // CALENDARS
    ///////////////////////////////////////////////////////////////////////////

    public Calendar getCalendar(UUID id)
    {
        return this.calendars.get(id);
    }

    public List<Calendar> getCalendars()
    {
        return new ArrayList<>(this.calendars.values());
    }

    public void removeACalendar(Calendar c)
    {
        this.calendars.remove(c.id);
        c.setApplication(null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // STATES
    ///////////////////////////////////////////////////////////////////////////

    private State dto2state(DTOState d, EventSourceContainer parent)
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

        return new State(this, d, parent, trFromState, trToState);
    }

    public State getState(UUID id)
    {
        for (EventSourceWrapper d : this.sources.values())
        {
            if (d.isContainer())
            {
                EventSourceContainer c = (EventSourceContainer) d.getSource();
                for (DTOState s : c.getContainedStates())
                {
                    if (s.getId().equals(id))
                    {
                        return dto2state(s, c);
                    }
                }
            }
        }
        throw new ChronixException("state not found");
    }

    public List<State> getStatesClientOfSource(UUID sourceId)
    {
        List<State> res = new ArrayList<>();

        for (EventSourceWrapper d : this.sources.values())
        {
            if (d.isContainer())
            {
                EventSourceContainer c = (EventSourceContainer) d.getSource();
                for (DTOState s : c.getContainedStates())
                {
                    if (s.getEventSourceId() == sourceId)
                    {
                        res.add(dto2state(s, c));
                    }
                }
            }
        }
        return res;
    }

}
