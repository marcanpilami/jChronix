package org.oxymores.chronix.engine.helpers;

import java.io.File;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;

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
        try
        {
            // Clean history db
            EntityManager em1 = ctx.getHistoryEM();
            EntityTransaction tr1 = em1.getTransaction();

            tr1.begin();
            em1.createQuery("DELETE FROM RunLog r").executeUpdate();
            em1.createQuery("DELETE FROM UserTrace r").executeUpdate();
            tr1.commit();
            em1.close();

            // Clean OP db
            EntityManager em2 = ctx.getTransacEM();
            EntityTransaction tr2 = em2.getTransaction();

            tr2.begin();
            em2.createQuery("DELETE FROM EnvironmentValue r").executeUpdate();
            em2.createQuery("DELETE FROM ClockTick r").executeUpdate();
            em2.createQuery("DELETE FROM EventConsumption r").executeUpdate();
            em2.createQuery("DELETE FROM TokenReservation r").executeUpdate();

            em2.createQuery("DELETE FROM Event r").executeUpdate();
            em2.createQuery("DELETE FROM PipelineJob r").executeUpdate();
            em2.createQuery("DELETE FROM CalendarPointer r").executeUpdate();
            em2.createQuery("DELETE FROM TranscientBase r").executeUpdate();

            em2.createQuery("DELETE FROM RunStats r").executeUpdate();
            em2.createQuery("DELETE FROM RunMetrics r").executeUpdate();
            tr2.commit();
            em2.close();
        }
        catch (Exception e)
        {
            log.warn("Purge has failed for a DB", e);
        }
    }
}
