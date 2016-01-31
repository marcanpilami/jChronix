package org.oxymores.chronix.core.context;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;

public class EventSourceRegistry
{
    private static ServiceTracker<EventSourceBehaviour, EventSourceBehaviour> tracker;

    static
    {
        Bundle bd = FrameworkUtil.getBundle(EventSourceRegistry.class);
        // Only if OSGI.
        if (bd != null)
        {
            tracker = new ServiceTracker<EventSourceBehaviour, EventSourceBehaviour>(bd.getBundleContext(), EventSourceBehaviour.class,
                    null);
            tracker.open();
        }
    }

    public static EventSourceBehaviour[] getAllEventSources()
    {
        if (tracker != null)
        {
            return (EventSourceBehaviour[]) tracker.getServices();
        }
        return new EventSourceBehaviour[0];
    }
}
