package org.oxymores.chronix.engine.helpers;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * Some methods not used at all for now.
 */
public final class ContextHelper
{
    private static final Logger log = LoggerFactory.getLogger(ContextHelper.class);

    private ContextHelper()
    {}

    public static void resetContext(ChronixContextMeta ctxMeta, ChronixContextTransient ctxDb)
    {
        // Clear metabase directory of all XML/AMQ files
        File[] fileList = ctxMeta.getRootMeta().listFiles();
        for (File ff : fileList)
        {
            if (!ff.getAbsolutePath().contains("db_") && !FileUtils.deleteQuietly(ff))
            {
                log.warn("Purge has failed for file " + ff.getAbsolutePath());
            }
        }

        // Clear JPA elements (do NOT simply remove the files - this would invalidate the context)
        try (Connection connHist = ctxDb.getHistoryDataSource().beginTransaction();
                Connection connTransac = ctxDb.getTransacDataSource().beginTransaction())
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

    private static final Pattern versionPattern = Pattern.compile("<modelVersion>(\\d+)</modelVersion>");

    public static int getFileVersion(File f)
    {
        LineIterator it = null;
        try
        {
            it = FileUtils.lineIterator(f);
            while (it.hasNext())
            {
                String line = it.nextLine();
                Matcher m = versionPattern.matcher(line);
                if (m.find())
                {
                    return Integer.parseInt(m.group(1));
                }
            }
            throw new ChronixInitializationException("no version defined in file " + f.getAbsolutePath());
        }
        catch (IOException | NumberFormatException | ChronixInitializationException e)
        {
            log.error("could not extract file version from file", e);
            throw new ChronixInitializationException("", e);
        }
        finally
        {
            LineIterator.closeQuietly(it);
        }
    }
}
