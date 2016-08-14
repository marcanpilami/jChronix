package org.oxymores.chronix.core.context.api;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.context.ContextHandler;
import org.oxymores.chronix.core.engine.api.OrderService;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.ResOrder;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixException;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

@Component
public class ApiOrder implements OrderService
{
    private static final Logger log = LoggerFactory.getLogger(ApiOrder.class);

    // The service works under an independent context.
    private String ctxMetaPath, ctxDbHistoryPath, ctxDbTransacPath;

    @Activate
    @Modified
    private void activate(Map<String, String> configuration)
    {
        ctxMetaPath = configuration.getOrDefault("chronix.repository.path", "./target/nodes/local");
        if (!(new File(ctxMetaPath).exists()))
        {
            throw new ChronixInitializationException(
                    "cannot create api service - directory " + ctxMetaPath + " does not exist. Check service configuration.");
        }
        ctxDbHistoryPath = FilenameUtils.concat(ctxMetaPath, "db_history/db");
        ctxDbTransacPath = FilenameUtils.concat(ctxMetaPath, "db_transac/db");
    }

    private ChronixContextTransient getCtxDb()
    {
        return ContextHandler.getDb(ctxDbHistoryPath, ctxDbTransacPath);
    }

    private ChronixContextMeta getMetaDb()
    {
        return ContextHandler.getMeta(ctxMetaPath);
    }

    @Override
    public ResOrder orderForceOK(UUID launchId)
    {
        try (Connection conn = this.getCtxDb().getHistoryDataSource().open())
        {
            RunLog rl = conn.createQuery("SELECT * FROM RunLog WHERE id=:id").addParameter("id", launchId)
                    .executeAndFetchFirst(RunLog.class);
            SenderHelpers.sendOrderForceOk(rl.getApplicationId(), rl.getId(), rl.getExecutionNodeId(), this.getMetaDb().getEnvironment());
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
            Application a = this.getMetaDb().getApplication(appId);
            Place p = this.getMetaDb().getEnvironment().getPlace(placeId);
            State s = a.getState(stateId);
            if (insidePlan)
            {
                try (Connection o = this.getCtxDb().getTransacDataSource().beginTransaction())
                {
                    SenderHelpers.runStateInsidePlan(s, p, o, null);
                }
            }
            else
            {
                SenderHelpers.runStateAlone(s, p);
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
        try (Connection conn = this.getCtxDb().getHistoryDataSource().open())
        {
            rl = conn.createQuery("SELECT * FROM RunLog WHERE id=:id").addParameter("id", launchId).executeAndFetchFirst(RunLog.class);
        }
        return orderLaunch(rl.getApplicationId(), rl.getStateId(), rl.getPlaceId(), false);
    }

    @Override
    public void resetCache()
    {
        ContextHandler.resetCtx();
    }

    @Override
    public ResOrder orderExternal(String externalSourceName, String externalData)
    {
        try
        {
            SenderHelpers.sendOrderExternalEvent(externalSourceName, externalData,
                    this.getMetaDb().getEnvironment().getNodesList().get(0).getName());
            return new ResOrder("OrderExternal", true, "Success");
        }
        catch (JMSException e)
        {
            log.error("could not send external order", e);
            return new ResOrder("OrderExternal", false, e.getMessage());
        }
    }
}
