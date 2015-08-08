package org.oxymores.chronix.wapi;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.ResOrder;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixException;

@Path("/live")
public class ServiceConsole
{
    private static Logger log = Logger.getLogger(ServiceConsole.class);

    private ChronixContext ctx = null;

    public ServiceConsole(ChronixContext ctx)
    {
        this.ctx = ctx;
    }

    @POST
    @Path("log")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HistoryQuery getLog(HistoryQuery q)
    {
        log.debug("getLog POST was called");

        if (q.getMarkedForRunAfter() == null)
        {
            q.setMarkedForRunAfter(DateTime.now().minusDays(1).toDate());
        }
        if (q.getMarkedForRunBefore() == null)
        {
            q.setMarkedForRunBefore(DateTime.now().plusDays(1).toDate());
        }

        EntityManager em = ctx.getHistoryEM();
        try
        {
            String sort = "";
            if (q.getSorts().size() > 0)
            {
                sort = " ORDER BY ";
                for (HistoryQuery.SortSpec s : q.getSorts())
                {
                    sort += " r." + s.col.getCoreLogField() + " " + s.order.name() + ",";
                }
                sort = sort.substring(0, sort.length() - 1);
            }

            TypedQuery<RunLog> l = em.createQuery("SELECT r FROM RunLog r WHERE r.markedForUnAt >= ?1 AND r.markedForUnAt <= ?2 " + sort, RunLog.class);
            l.setParameter(1, q.getMarkedForRunAfter());
            l.setParameter(2, q.getMarkedForRunBefore());

            if (q.getStartLine() != null)
            {
                l.setFirstResult(q.getStartLine());
            }
            if (q.getPageSize() != null)
            {
                l.setMaxResults(q.getPageSize());
            }

            List<DTORunLog> res = new ArrayList<>();
            for (RunLog rl : l.getResultList())
            {
                res.add(CoreToDto.getDTORunLog(rl));
            }
            q.setRes(res);
            q.setTotalLogs((long) em.createQuery("SELECT COUNT(l) FROM RunLog l").getSingleResult());
        }
        finally
        {
            em.close();
        }

        log.debug("End of call to getLog - returning " + q.getRes().size() + " logs out of a total of " + q.getTotalLogs());
        return q;
    }

    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DTORunLog> getLog()
    {
        log.debug("Service getLog was called");
        EntityManager em = null;
        List<DTORunLog> res = null;

        try
        {
            em = ctx.getHistoryEM();

            TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt", RunLog.class);
            res = new ArrayList<>();

            for (RunLog rl : q.getResultList())
            {
                res.add(CoreToDto.getDTORunLog(rl));
            }
            log.debug("End of call to getLog - returning " + res.size());
        }
        finally
        {
            em.close();
        }

        return res;
    }

    @GET
    @Path("/shortlog/{id}")
    @Produces("text/plain")
    public String getShortLog(@PathParam("id") UUID id)
    {
        log.debug("Service getShortLog was called for ID " + id.toString());
        EntityManager em = ctx.getHistoryEM();
        RunLog rl = em.find(RunLog.class, id);
        em.close();

        if (rl == null)
        {
            log.debug("Service getShortLog has ended without finding the log");
            return "notfound";
        }
        else
        {
            log.debug("Service getShortLog has ended - the log was found");
            return rl.getShortLog().substring(Math.min(rl.getShortLog().length(), 2), rl.getShortLog().length());
        }
    }

    @GET
    @Path("/logssince/{a}")
    public List<DTORunLog> getLogSince(@PathParam("a") Date since)
    {
        log.debug("Service getLogSince was called");
        EntityManager em = ctx.getHistoryEM();
        TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r WHERE r.lastLocallyModified > ?1 ORDER BY r.enteredPipeAt",
                RunLog.class);
        q.setParameter(1, since);
        ArrayList<DTORunLog> res = new ArrayList<>();

        for (RunLog rl : q.getResultList())
        {
            res.add(CoreToDto.getDTORunLog(rl));
        }

        em.close();
        log.debug("End of call to getLogSince - returning " + res.size());
        return res;
    }

    @GET
    @Path("/order/forceok/{launchId}")
    @Produces("application/json")
    public ResOrder orderForceOK(@PathParam("launchId") String launchId)
    {
        log.debug("Service orderForceOK was called");
        try
        {
            EntityManager em = ctx.getHistoryEM();
            RunLog rl = em.find(RunLog.class, launchId);
            em.close();
            SenderHelpers.sendOrderForceOk(rl.getApplicationId(), rl.getId(), rl.getExecutionNodeId(), ctx);
        }
        catch (ChronixException e)
        {
            log.debug("End of call to orderForceOK - failure");
            return new ResOrder("ForceOK", false, e.toString());
        }
        log.debug("End of call to orderForceOK - success");
        return new ResOrder("ForceOK", true, "The order was sent successfuly");
    }

    @GET
    @Path("/logfile/{launchId}")
    @Produces("text/plain; charset=utf-8")
    public File getLogFile(@PathParam("launchId") String launchId)
    {
        EntityManager em = ctx.getHistoryEM();
        RunLog rl = em.find(RunLog.class, launchId);
        em.close();

        File f = new File(rl.getLogPath());
        return f;
    }

    @GET
    @Path("/order/launch/outofplan/{appId}/{stateId}/{placeId}")
    @Produces("application/json")
    public ResOrder orderLaunchOutOfPlan(@PathParam("appId") UUID appId, @PathParam("stateId") UUID stateId,
            @PathParam("placeId") UUID placeId)
    {
        try
        {
            Application a = ctx.getApplication(appId);
            Place p = this.ctx.getNetwork().getPlace(placeId);
            State s = a.getState(stateId);
            SenderHelpers.runStateAlone(s, p, ctx);
        }
        catch (Exception e)
        {
            return new ResOrder("LaunchOutOfPlan", false, e.getMessage());
        }
        return new ResOrder("LaunchOutOfPlan", true, "The order was sent successfuly");
    }

    @GET
    @Path("/order/launch/outofplan/duplicatelaunch/{launchId}")
    @Produces("application/json")
    public ResOrder orderLaunchOutOfPlan(@PathParam("launchId") UUID launchId)
    {
        log.debug("Service orderLaunchOutOfPlan was called");
        EntityManager em = ctx.getHistoryEM();
        RunLog rl = em.find(RunLog.class, launchId);
        em.close();
        return orderLaunchOutOfPlan(UUID.fromString(rl.getApplicationId()), UUID.fromString(rl.getStateId()),
                UUID.fromString(rl.getPlaceId()));
    }
}
