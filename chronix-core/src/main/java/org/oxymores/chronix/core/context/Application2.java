package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.exceptions.ChronixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class Application2 implements IMetaSource
{
    @XStreamOmitField
    static final BundleContext osgiCtx = FrameworkUtil.getBundle(Application2.class).getBundleContext();
    private static final Logger log = LoggerFactory.getLogger(Application2.class);

    static final int currentModelVersion = 1;
    static final int compatibleUpToBackwards = 0;

    @Min(0)
    private int modelVersion = currentModelVersion;
    private DateTime latestSave = DateTime.now();
    private List<ApplicationVersion> versions = new ArrayList<>();

    @NotNull
    protected UUID id = UUID.randomUUID();
    @NotNull
    @Size(min = 1, max = 50)
    protected String name;
    @NotNull
    @Size(min = 1, max = 255)
    protected String description;

    // The sources must NOT be serialised. Each plugin is responsible for its own serialisation.
    private transient Map<UUID, EventSourceComplete> sources = new HashMap<>();

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
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner class helpers
    ///////////////////////////////////////////////////////////////////////////

    /**
     * An event source associated to its behavior
     */
    private class EventSourceComplete
    {
        private EventSourceComplete(DTO d, EventSourceBehaviour b)
        {
            this.eventSource = d;
            this.behaviour = b;
        }

        private DTO eventSource;
        private EventSourceBehaviour behaviour;
    }

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
    public DTO getEventSource(UUID id)
    {
        if (!this.containsSource(id))
        {
            throw new ChronixException("non existing source");
        }
        return this.sources.get(id).eventSource;
    }

    @SuppressWarnings("unchecked")
    public <T extends DTO> List<T> getEventSources(Class<T> klass)
    {
        List<T> res = new ArrayList<>();
        for (EventSourceComplete d : this.sources.values())
        {
            if (d != null && klass.isInstance(d.eventSource))
            {
                res.add((T) d.eventSource);
            }
        }
        return res;
    }

    /**
     * Returns a copy of the event source list.
     */
    public List<DTO> getEventSources()
    {
        List<DTO> res = new ArrayList<>();
        for (EventSourceComplete s : this.sources.values())
        {
            res.add(s.eventSource);
        }
        return res;
    }

    @Override
    public <T extends DTO & Serializable> void registerSource(T source, EventSourceBehaviour service)
    {
        log.debug("Registering event source with ID " + source.getId() + " associated to service " + service.getClass().getSimpleName()
                + " - " + this.toString());
        EventSourceComplete esc = new EventSourceComplete(source, service);
        sources.put(source.getId(), esc);
    }

    @Override
    public <T extends DTO> void unregisterSource(T source)
    {
        if (this.sources.containsKey(source.getId()))
        {
            this.sources.remove(source.getId());
        }
    }

    public boolean containsSource(DTO source)
    {
        return this.containsSource(source.getId());
    }

    public boolean containsSource(UUID id)
    {
        return this.sources.containsKey(id);
    }

    public EventSourceBehaviour getEventSourceBehaviour(UUID id)
    {
        if (!this.containsSource(id))
        {
            throw new ChronixException("non existing source");
        }
        return this.sources.get(id).behaviour;
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
}
