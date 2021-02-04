/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oxymores.chronix.engine.helpers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.SQLException;

import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

/**
 *
 * @author Marc-Antoine
 */
public class DbUpgrader
{
    private static final Logger log = LoggerFactory.getLogger(DbUpgrader.class);

    public enum DbType
    {
        TP, HIST;
    }

    public enum DbEngine
    {
        HSQLDB;
    }

    public static void upgradeDb(Sql2o fact, DbType type, DbEngine engine)
    {
        int db_version;
        try (Connection conn = fact.open())
        {
            db_version = conn.createQuery("SELECT MAX(ID) FROM VERSION").executeScalar(Integer.class);
            log.debug("Database is in version {}", db_version);
        }
        catch (Exception e)
        {
            log.info("Database has no version indication, it will therefore be created");
            runScript(fact, "sql/" + type.name().toLowerCase() + "/" + engine.name().toLowerCase() + "/000000.sql");
            setVersion(fact, 0);
        }
    }

    public static void runScript(Sql2o fact, String scriptPath)
    {
        log.debug("Trying to run database script " + scriptPath);
        SqlFile f;
        try
        {
            Reader r = new InputStreamReader(DbUpgrader.class.getClassLoader().getResourceAsStream(scriptPath));
            f = new SqlFile(r, "", null, "UTF8", false, (URL)null);
        }
        catch (IOException ex)
        {
            log.error("Could not open SQL file", ex);
            throw new ChronixInitializationException("", ex);
        }

        try (Connection conn = fact.open())
        {
            f.setConnection(conn.getJdbcConnection());
            f.execute();
        }
        catch (SqlToolError | SQLException ex)
        {
            log.error("Could not run SQL script file", ex);
            throw new ChronixInitializationException("", ex);
        }

        log.info("File " + scriptPath + " was correctly run on database");
    }

    public static void setVersion(Sql2o fact, int version)
    {
        try (Connection conn = fact.beginTransaction())
        {
            conn.createQuery("INSERT INTO VERSION(ID, APPLIED) VALUES(:id, now())").addParameter("id", version).executeUpdate();
            conn.commit();
            log.info("Database version was set to " + version);
        }
    }
}
