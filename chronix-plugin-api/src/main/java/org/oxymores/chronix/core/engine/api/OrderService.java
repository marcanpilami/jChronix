package org.oxymores.chronix.core.engine.api;

import java.util.UUID;

import org.oxymores.chronix.dto.ResOrder;

/**
 * The service is the only public way of interacting with the scheduling engine itself: launch new jobs, kill a running job, ...
 */
public interface OrderService
{
    /**
     * Takes a KO launch and tells the scheduler to do as if it actually had correctly ended. Often used to force the rest of a chain to run
     * despite a crash.
     */
    public ResOrder orderForceOK(UUID launchId);

    /**
     * Main method to trigger an immediate launch.
     * 
     * @param appId
     * @param stateId
     * @param placeId
     * @param insidePlan
     * @param fieldOverload
     *            can be null
     * @return
     */
    public ResOrder orderLaunch(UUID appId, UUID stateId, UUID placeId, Boolean insidePlan);

    /**
     * Reruns an ended launch. This cannot have any consequence on any plan.
     * 
     * @param launchId
     * @return
     */
    public ResOrder duplicateEndedLaunchOutOfPlan(UUID launchId);

    public void resetCache();
    
    public ResOrder orderExternal(String externalSourceName, String externalData);
}
