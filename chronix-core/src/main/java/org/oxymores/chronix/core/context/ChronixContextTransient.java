package org.oxymores.chronix.core.context;

import java.sql.SQLException;

import org.hsqldb.jdbc.JDBCPool;
import org.oxymores.chronix.engine.helpers.DbUpgrader;
import org.oxymores.chronix.engine.helpers.UUIDQuirk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

public class ChronixContextTransient
{
    private static final Logger log = LoggerFactory.getLogger(ChronixContextTransient.class);

    private final String historyDbPath, transacDbPath, historyDbUrl, transacDbUrl;
    private Sql2o historyDS, transacDS;

    public ChronixContextTransient(String historyDBPath, String transacDbPath)
    {
        this.historyDbPath = historyDBPath;
        this.transacDbPath = transacDbPath;
        this.historyDbUrl = "jdbc:hsqldb:file:" + this.historyDbPath;
        this.transacDbUrl = "jdbc:hsqldb:file:" + this.transacDbPath;
    }

    public Sql2o getTransacDataSource()
    {
        if (transacDS != null)
        {
            return transacDS;
        }
        log.info("Opening database at " + this.transacDbUrl);
        JDBCPool ds = new JDBCPool(10);
        ds.setUrl(this.transacDbUrl);
        transacDS = new Sql2o(ds, new UUIDQuirk());
        DbUpgrader.upgradeDb(transacDS, DbUpgrader.DbType.TP, DbUpgrader.DbEngine.HSQLDB);
        return transacDS;
    }

    public Sql2o getHistoryDataSource()
    {
        if (historyDS != null)
        {
            return historyDS;
        }
        log.info("Opening database at " + this.historyDbUrl);
        JDBCPool ds = new JDBCPool(10);
        ds.setUrl(this.historyDbUrl);
        historyDS = new Sql2o(ds, new UUIDQuirk());
        DbUpgrader.upgradeDb(historyDS, DbUpgrader.DbType.HIST, DbUpgrader.DbEngine.HSQLDB);
        return historyDS;
    }

    public void close()
    {
        log.debug("Context " + this.toString() + " is closing");
        if (this.historyDS != null)
        {
            try (Connection conn = this.historyDS.open())
            {
                conn.createQuery("SHUTDOWN").executeUpdate();
                log.debug("History database closed");
            }
            catch (Exception e)
            {
                if (!(e instanceof Sql2oException && e.getMessage().contains("connection exception: closed")))
                // double closing may happen during tests when shutting everything down at once.
                {
                    log.warn("Could not close history database on context destruction", e);
                }
            }
            try
            {
                ((JDBCPool) this.historyDS.getDataSource()).close(0);
                this.historyDS = null;
            }
            catch (SQLException ex)
            {
                log.warn("Could not clean JDBC object related to the history database, even if the database itself was shut down", ex);
            }
        }
        if (this.transacDS != null)
        {
            try (Connection conn = this.transacDS.open())
            {
                conn.createQuery("SHUTDOWN").executeUpdate();
                log.debug("Transac database closed");
            }
            catch (Exception e)
            {
                if (!(e instanceof Sql2oException && e.getMessage().contains("connection exception: closed")))
                {
                    log.warn("Could not close transac database on context destruction", e);
                }
            }
            try
            {
                ((JDBCPool) this.transacDS.getDataSource()).close(0);
                this.transacDS = null;
            }
            catch (SQLException ex)
            {
                log.warn("Could not clean JDBC object related to the transac database, even if the database itself was shut down", ex);
            }
        }
        log.debug("Context " + this.toString() + " has closed");
    }
}
