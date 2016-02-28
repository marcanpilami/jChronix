package org.oxymores.chronix.core.source.api;

import java.io.Serializable;
import java.util.UUID;

/**
 * An <strong>event source</strong> is a node of the production plan graph, that is an item that can be triggered, then runs and returns a
 * result from which events will be created (the event creation is taken care of by the engine itself, not the source which only returns a
 * "run result" object). Examples are aplenty among the event sources bundled with the default distribution: a shell command, a clock, a
 * file incoming inside a directory...<br>
 * <br>
 * Instances of classes extending EventSource can be either directly created by clients (through a constructor) or deserialised from
 * persistent storage by an {@link EventSourceProvider}. The EventSource has a pointer to the {@link EventSourceProvider} which handles
 * serialisation/deserialisation for itself.<br>
 * <br>
 * Event sources are expected to behave like <strong>Data Transfer Objects (DTO)</strong> - that is, they should be as stable as possible
 * between versions, easy to create, easy to serialise (no complex graph).<br>
 * Note this is not an interface but a base abstract class, so as to allow easier ascending compatibility in the event of an evolution of
 * the core model.<br>
 * <br>
 * This class should not be used directly - rather, one of its subclasses should be subclassed. Each expose a different set of event source
 * capabilities:
 * <ul>
 * <li>{@link EventSourceTriggered} is the most common type of event source: it denotes something that is launched by the engine when it has
 * decided it should run (due to events coming from other sources).</li>
 * <li>{@link EventSourceContainer} is a special type of {@link EventSourceTriggered} that can contain other elements.</li>
 * <li>{@link EventSourceSelfTriggered} is an uncommon type of event sources which are hosted by the engine and which create events when
 * they want to. Typical examples are sources which regularly (every minute, ...) create events.</li>
 * <li>{@link EventSourceExternalyTriggered} denotes events that are directly created by external systems without any intervention of the
 * engine. An example would be a file transfer middleware which signals the engine a file has just arrived.</li>
 * </ul>
 * 
 * 
 */
public abstract class EventSource implements Serializable
{
    private static final long serialVersionUID = -1239160226415660389L;

    protected UUID id = UUID.randomUUID();

    /**
     * Every event source instance has an unique ID. We use statistically unique UUID since this is an easy solution for uniqueness in
     * distributed systems like Chronix.
     */
    public UUID getId()
    {
        return this.id;
    }

    /**
     * A name, less than 20 characters.
     */
    public abstract String getName();

    /**
     * The {@link EventSourceProvider} that should be used for persistence.
     */
    public abstract Class<? extends EventSourceProvider> getProvider();

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis

    /**
     * The engine calls this method to get the correct interpretation of an event and a transition definition. This is the one method that
     * "gives meaning" to event data (the very same data that was created by this source).<br>
     * It is given a transition originating from this source, the event (created by this source) to analyse, and the place on which it
     * should be analysed.<br>
     * The method must focus on giving meaning to the event and transition data - it should not take anything else into account (such as
     * calendars, tokens...)<br>
     * <br>
     * The default implementation simply checks eventData == transitionGuard (for non null guards only).
     */
    public boolean isTransitionPossible(DTOTransition tr, DTOEvent event)
    {
        // Check guards
        if (tr.getGuard1() != null && !tr.getGuard1().equals(event.getConditionData1()))
        {
            return false;
        }
        if (tr.getGuard2() != null && !tr.getGuard2().equals(event.getConditionData2()))
        {
            return false;
        }
        if (tr.getGuard3() != null && !tr.getGuard3().equals(event.getConditionData3()))
        {
            return false;
        }
        if (tr.getGuard4() != null && !tr.getGuard4().equals(event.getConditionData4()))
        {
            return false;
        }
        return true;
    }

    // Event analysis
    ///////////////////////////////////////////////////////////////////////////
}
