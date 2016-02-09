package org.oxymores.chronix.core.source.api;

import org.joda.time.DateTime;

/**
 * An event source which creates events on its own - without being triggered by events created by other event sources. For example, an event
 * source may run every minute.<br>
 * An event source implementing this interface is not expected to actually implement the various "run" methods - it can just throw an
 * NotImplementedException.
 * 
 */
public interface EventSourceOptionSelfTriggered
{
    /**
     * This is the main method of a self-triggered event source. It is responsible for actually creating events.<br>
     * On each call, this method returns the time at which it should be called again. (e.g.: a clock creating an event every minute will
     * return "current time+1mn").<br>
     * This method is called at engine startup, and then called at the time returned by the previous call.<br>
     * Please note that the engine never ever uses the system time (so as to allow simulations), so this method should not do so either -
     * only use the time given in argument for "current time". It also means that the time given in argument is always *exactly* the time
     * returned by the previous call (and not an approximation by at least a few milliseconds as would have been the case if system time was
     * used).
     */
    public DateTime selfTrigger();
}
