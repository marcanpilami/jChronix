package org.oxymores.chronix.engine.helpers;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.sql2o.Connection;

/**
 Not used at all for now.
 */
public final class ContextHelper
{
    private static final Logger log = Logger.getLogger(ContextHelper.class);

    private ContextHelper()
    {
    }

    public static void resetContext(ChronixContext ctx)
    {
        // Clear metabase directory of all XML/AMQ files
        File[] fileList = new File(ctx.getContextRoot()).listFiles();
        for (File ff : fileList)
        {
            if (!ff.getAbsolutePath().contains("db_") && !FileUtils.deleteQuietly(ff))
            {
                log.warn("Purge has failed for file " + ff.getAbsolutePath());
            }
        }

        // Clear JPA elements (do NOT simply remove the files - this would invalidate the context)
        try (Connection connHist = ctx.getHistoryDataSource().beginTransaction(); Connection connTransac = ctx.getTransacDataSource().beginTransaction())
        {
            // Clean history db
            connHist.createQuery("DELETE FROM RunLog r").executeUpdate();
            connHist.createQuery("DELETE FROM UserTrace r").executeUpdate();

            // Clean OP db
            connHist.createQuery("DELETE FROM EnvironmentValue r").executeUpdate();
            connHist.createQuery("DELETE FROM ClockTick r").executeUpdate();
            connHist.createQuery("DELETE FROM EventConsumption r").executeUpdate();
            connHist.createQuery("DELETE FROM TokenReservation r").executeUpdate();

            connHist.createQuery("DELETE FROM Event r").executeUpdate();
            connHist.createQuery("DELETE FROM PipelineJob r").executeUpdate();
            connHist.createQuery("DELETE FROM CalendarPointer r").executeUpdate();
            connHist.createQuery("DELETE FROM TranscientBase r").executeUpdate();

            connHist.createQuery("DELETE FROM RunStats r").executeUpdate();
            connHist.createQuery("DELETE FROM RunMetrics r").executeUpdate();
        }
        catch (Exception e)
        {
            log.warn("Purge has failed for a DB", e);
        }
    }
}
