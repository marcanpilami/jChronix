package org.oxymores.chronix.engine.helpers;

import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.engine.api.OrderService;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.ResOrder;
import org.oxymores.chronix.exceptions.ChronixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

@Component
public class ApiOrder implements OrderService
{
    private static final Logger log = LoggerFactory.getLogger(ApiOrder.class);

    // The service works under an independent context.
    private ChronixContext ctx;

    @Activate
    private void activate(ComponentContext cc)
    {
        // TODO: use configuration. Especially, we need a correct LOCAL NODE for JMS connections to work.
        ctx = new ChronixContext("local", "C:\\TEMP\\db1", false, "C:\\TEMP\\db1\\db_history\\db", "C:\\TEMP\\db1\\db_transac\\db");
    }

    public ApiOrder()
    {
        // Default constructor for OSGI injection
    }

    public ApiOrder(ChronixContext ctx)
    {
        // Specific constructor for non-OSGI environments
        this.ctx = ctx;
    }

    @Override
    public ResOrder orderForceOK(UUID launchId)
    {
        try (Connection conn = this.ctx.getHistoryDataSource().open())
        {
            RunLog rl = conn.createQuery("SELECT * FROM RunLog WHERE id=:id").addParameter("id", launchId)
                    .executeAndFetchFirst(RunLog.class);
            SenderHelpers.sendOrderForceOk(rl.getApplicationId(), rl.getId(), rl.getExecutionNodeId(), this.ctx);
        }
        catch (ChronixException e)
        {
            log.debug("End of call to orderForceOK - failure");
            return new ResOrder("ForceOK", false, e.toString());
        }
        log.debug("End of call to orderForceOK - success");
        return new ResOrder("ForceOK", true, "The order was sent successfuly");
    }

    @Override
    public ResOrder orderLaunch(UUID appId, UUID stateId, UUID placeId, Boolean insidePlan)
    {
        try
        {
            Application a = this.ctx.getApplication(appId);
            Place p = this.ctx.getEnvironment().getPlace(placeId);
            State s = a.getState(stateId);
            if (insidePlan)
            {
                try (Connection o = this.ctx.getTransacDataSource().beginTransaction())
                {
                    SenderHelpers.runStateInsidePlan(s, p, this.ctx, o);
                }
            }
            else
            {
                SenderHelpers.runStateAlone(s, p, this.ctx);
            }
        }
        catch (Exception e)
        {
            log.warn("could not create an OOPL", e);
            return new ResOrder("LaunchOutOfPlan", false, e.getMessage());
        }
        return new ResOrder("LaunchOutOfPlan", true, "The order was sent successfuly");
    }

    @Override
    public ResOrder duplicateEndedLaunchOutOfPlan(UUID launchId)
    {
        RunLog rl;
        try (Connection conn = this.ctx.getHistoryDataSource().open())
        {
            rl = conn.createQuery("SELECT * FROM RunLog WHERE id=:id").addParameter("id", launchId).executeAndFetchFirst(RunLog.class);
        }
        return orderLaunch(rl.getApplicationId(), rl.getStateId(), rl.getPlaceId(), false);
    }

}
