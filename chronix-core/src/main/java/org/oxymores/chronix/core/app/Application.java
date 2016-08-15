package org.oxymores.chronix.core.app;

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
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.exceptions.ChronixException;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application implements Serializable
{
    private static final long serialVersionUID = -7565792688611748679L;
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    static final BundleContext osgiCtx = FrameworkUtil.getBundle(Application.class).getBundleContext();

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
    private Map<UUID, FunctionalSequence> calendars = new HashMap<>();

    // The sources
    private Map<UUID, EventSourceDef> sources = new HashMap<>();

    private Map<UUID, ParameterDef> sharedParameters = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    public Application()
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
        if (sharedParameters == null)
        {
            sharedParameters = new HashMap<>();
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

    public EventSourceDef getEventSource(UUID id)
    {
        if (!this.containsSource(id))
        {
            throw new ChronixException("non existing source");
        }
        return this.sources.get(id);
    }

    public EventSourceDef getEventSourceByName(String name)
    {
        for (EventSourceDef esd : this.sources.values())
        {
            if (esd.getName().equals(name))
            {
                return esd;
            }
        }
        throw new ChronixException("non existing source");
    }

    public Map<UUID, EventSourceDef> getEventSources()
    {
        return this.sources;
    }

    public boolean containsSource(UUID id)
    {
        return this.sources.containsKey(id);
    }

    public List<EventSourceDef> getEventSources(EventSourceProvider pr)
    {
        List<EventSourceDef> res = new ArrayList<>();
        for (EventSourceDef s : this.sources.values())
        {
            if (s.isProvidedBy(pr.getClass()))
            {
                res.add(s);
            }
        }
        return res;
    }

    public void removeSource(UUID id)
    {
        this.sources.remove(id);
    }

    public void addSource(DTOEventSource src, ChronixContextMeta ctx)
    {
        EventSourceDef w = new EventSourceDef(src, ctx, this);
        this.sources.put(src.getId(), w);
    }

    public void waitForAllPlugins()
    {
        Set<String> plugins = new HashSet<>();
        for (EventSourceDef w : this.sources.values())
        {
            plugins.add(w.getPluginSymbolicName());
        }
        log.info("Application " + this.name + " has " + this.sources.size() + " sources coming from the following plugins: "
                + plugins.toString());

        ServiceTracker<EventSourceProvider, EventSourceProvider> tracker = new ServiceTracker<EventSourceProvider, EventSourceProvider>(
                FrameworkUtil.getBundle(Application.class).getBundleContext(), EventSourceProvider.class, null);
        int maxWaitSec = 60;
        int waitedSec = 0;
        long waitStepSec = 1L;

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
    // PARAMETERS
    ///////////////////////////////////////////////////////////////////////////

    public ParameterDef getSharedParameter(UUID key)
    {
        return this.sharedParameters.get(key);
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

    public void setLatestSave(DateTime latestSave)
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

    public FunctionalSequence getCalendar(UUID id)
    {
        return this.calendars.get(id);
    }

    public List<FunctionalSequence> getCalendars()
    {
        return new ArrayList<>(this.calendars.values());
    }

    public void removeACalendar(FunctionalSequence c)
    {
        this.calendars.remove(c.id);
        c.setApplication(null);
    }

    public void addCalendar(FunctionalSequence seq)
    {
        this.calendars.put(seq.getId(), seq);
        seq.setApplication(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // STATES
    ///////////////////////////////////////////////////////////////////////////

    public State getState(UUID id)
    {
        for (EventSourceDef d : this.sources.values())
        {
            if (d.isContainer())
            {
                for (State s : d.getContainedStates())
                {
                    if (s.getId().equals(id))
                    {
                        return s;
                    }
                }
            }
        }
        throw new ChronixException("state not found");
    }

    /**
     * A list of states using the given source.
     * 
     * @param sourceId
     * @return
     */
    public List<State> getStatesClientOfSource(UUID sourceId)
    {
        List<State> res = new ArrayList<>();

        for (EventSourceDef d : this.sources.values())
        {
            if (d.isContainer())
            {
                for (State s : d.getContainedStates())
                {
                    if (s.getRepresentsId().equals(sourceId))
                    {
                        res.add(s);
                    }
                }
            }
        }
        return res;
    }

    public List<State> getStates()
    {
        List<State> res = new ArrayList<>();
        for (EventSourceDef esd : this.sources.values())
        {
            if (!esd.isContainer())
            {
                continue;
            }

            res.addAll(esd.getContainedStates());
        }

        return res;
    }

}
