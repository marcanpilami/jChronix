package org.oxymores.chronix.core.context.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.context.ContextHandler;
import org.oxymores.chronix.core.engine.api.HistoryService;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.HistoryQuery;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

@Component
public class ApiHistory implements HistoryService
{
    private static final Logger log = LoggerFactory.getLogger(ApiHistory.class);

    private String ctxMetaPath, ctxDbHistoryPath;

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
    }

    private ChronixContextTransient getCtxDb()
    {
        return ContextHandler.getDb(ctxDbHistoryPath, "_");
    }

    @Override
    public HistoryQuery query(HistoryQuery q)
    {
        if (q.getMarkedForRunAfter() == null)
        {
            q.setMarkedForRunAfter(DateTime.now().minusDays(1).toDate());
        }
        if (q.getMarkedForRunBefore() == null)
        {
            q.setMarkedForRunBefore(DateTime.now().plusDays(1).toDate());
        }

        // TODO: direct to DTO attempt!
        try (Connection conn = getCtxDb().getHistoryDataSource().open())
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

            String pagination = "";
            if (q.getPageSize() != null)
            {
                pagination += " LIMIT " + q.getPageSize();
            }
            if (q.getStartLine() != null)
            {
                pagination += " OFFSET " + q.getStartLine();
            }

            String rc = "";
            if (q.getResultCode() != null)
            {
                rc = " AND resultCode = :rc ";
            }

            String ended = "";
            if (q.hasEnded() != null && q.hasEnded())
            {
                ended = " AND stoppedRunningAt IS NOT NULL ";
            }
            if (q.hasEnded() != null && !q.hasEnded())
            {
                ended = " AND stoppedRunningAt IS NULL ";
            }

            Query qu = conn
                    .createQuery(
                            "SELECT * FROM RunLog r WHERE r.visible = 1 AND r.markedForUnAt >= :markedAfter AND r.markedForUnAt <= :markedBefore "
                                    + rc + ended + sort + pagination)
                    .addParameter("markedAfter", q.getMarkedForRunAfter()).addParameter("markedBefore", q.getMarkedForRunBefore());
            if (q.getResultCode() != null)
            {
                qu.addParameter("rc", q.getResultCode());
            }

            List<DTORunLog> res = new ArrayList<>();
            for (RunLog rl : qu.executeAndFetch(RunLog.class))
            {
                res.add(CoreToDto.getDTORunLog(rl));
            }
            q.setRes(res);
            q.setTotalLogs((long) conn.createQuery("SELECT COUNT(1) FROM RunLog").executeScalar(Long.class));
        }

        return q;
    }

    @Override
    public String getShortLog(UUID id)
    {
        String res;

        try (Connection conn = this.getCtxDb().getHistoryDataSource().open())
        {
            res = conn.createQuery("SELECT shortLog FROM RunLog WHERE id=:id").addParameter("id", id).executeScalar(String.class);
        }

        if (res == null)
        {
            log.debug("Service getShortLog has ended without finding the log");
            return "";
        }
        else
        {
            log.debug("Service getShortLog has ended - the log was found");
            return res;
        }
    }

    @Override
    public File getLogFile(UUID launchId)
    {
        String path;
        try (Connection conn = this.getCtxDb().getHistoryDataSource().open())
        {
            path = conn.createQuery("SELECT logPath FROM RunLog WHERE id=:id").addParameter("id", launchId).executeScalar(String.class);
        }

        log.debug("Log file was required at {}", path);
        return new File(path);
    }
}
