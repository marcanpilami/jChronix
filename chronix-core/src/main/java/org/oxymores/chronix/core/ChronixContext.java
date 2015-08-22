/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.oxymores.chronix.core;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.transactional.ClockTick;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.hsqldb.jdbc.JDBCPool;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.core.transactional.TokenReservation;
import org.oxymores.chronix.engine.helpers.DbUpgrader;
import org.oxymores.chronix.engine.helpers.UUIDQuirk;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

public final class ChronixContext
{
    private static final Logger log = LoggerFactory.getLogger(ChronixContext.class);
    private static final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private static final XStream xmlUtility = new XStream(new StaxDriver());

    // Persistence roots/datasources
    private Sql2o historyDS, transacDS;

    // Data needed to load the applications
    private final DateTime loaded;
    private final File configurationDirectory;
    private final String historyDbPath, transacDbPath, historyDbUrl, transacDbUrl;

    // Network
    private Network network;

    // Loaded applications
    private final Map<UUID, Application> applicationsById, stagedApplicationsById;
    private final Map<String, Application> applicationsByName;

    // Local node identification
    private String localNodeName;
    private transient ExecutionNode localNode;

    // Simulation data
    private boolean simulateExternalPayloads = false;

    static
    {
        xmlUtility.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
    }

    /**
     * Creates a new Context.
     *
     * @param localNodeName
     * @param appConfDirectory
     * @param simulation
     * @param historyDBPath
     * @param transacDbPath
     */
    public ChronixContext(String localNodeName, String appConfDirectory, boolean simulation, String historyDBPath, String transacDbPath)
    {
        this.loaded = DateTime.now();
        this.localNodeName = localNodeName;
        this.configurationDirectory = new File(FilenameUtils.normalize(appConfDirectory));
        this.applicationsById = new HashMap<>();
        this.stagedApplicationsById = new HashMap<>();
        this.applicationsByName = new HashMap<>();
        this.historyDbPath = historyDBPath;
        this.transacDbPath = transacDbPath;
        this.setSimulation(simulation);

        if (simulation)
        {
            this.historyDbUrl = "jdbc:hsqldb:mem:" + UUID.randomUUID();
            this.transacDbUrl = "jdbc:hsqldb:mem:" + UUID.randomUUID();
        }
        else
        {
            this.historyDbUrl = "jdbc:hsqldb:file:" + this.historyDbPath;
            this.transacDbUrl = "jdbc:hsqldb:file:" + this.transacDbPath;
        }

        if (historyDBPath == null && !simulation)
        {
            // Runner without metabase - don't load anything
            return;
        }

        if (!this.configurationDirectory.canRead())
        {
            throw new ChronixInitializationException("metabase is not readable");
        }

        this.loadContext();

        if (!"simu".equals(localNodeName) && this.getLocalNode() == null)
        {
            throw new ChronixInitializationException("there is no node named " + localNodeName + " described inside the metabase");
        }
    }

    private void loadContext()
    {
        log.info(String.format("Creating a new context from configuration database %s", this.configurationDirectory));

        // List files in directory - and therefore applications
        File[] fileList = this.configurationDirectory.listFiles();
        HashMap<String, File> toLoad = new HashMap<>();
        HashMap<String, File> toLoadWorking = new HashMap<>();

        for (File f : fileList)
        {
            String fileName = f.getName();

            if (fileName.startsWith("app_") && fileName.endsWith(".crn") && fileName.contains("CURRENT") && fileName.contains("data"))
            {
                // This is a current app DATA file.
                String id = fileName.split("_")[2];
                if (!toLoad.containsKey(id))
                {
                    toLoad.put(id, f);
                }
            }

            if (fileName.startsWith("app_") && fileName.endsWith(".crn") && fileName.contains("WORKING") && fileName.contains("data"))
            {
                String id = fileName.split("_")[2];
                if (!toLoadWorking.containsKey(id))
                {
                    toLoadWorking.put(id, f);
                }
            }
        }

        // ///////////////////
        // Load network
        // ///////////////////
        this.network = this.loadNetwork();

        // ///////////////////
        // Load apps
        // ///////////////////
        for (File ss : toLoad.values())
        {
            Application app = this.loadApplication(ss, this.simulateExternalPayloads);
            applicationsById.put(app.getId(), app);
            applicationsByName.put(app.getName(), app);
        }
        for (File ss : toLoadWorking.values())
        {
            Application app = this.loadApplication(ss, this.simulateExternalPayloads);
            app.isFromCurrentFile(false);
            stagedApplicationsById.put(app.getId(), app);
        }

        // ///////////////////
        // Post load checkup & inits
        // ///////////////////
        if (this.applicationsById.values().size() > 0)
        {
            try (Connection conn = this.getTransacDataSource().beginTransaction())
            {
                for (Application a : this.applicationsById.values())
                {
                    a.isFromCurrentFile(true);
                    for (Calendar c : a.calendars.values())
                    {
                        c.createPointers(conn);
                    }
                    for (PlaceGroup g : a.getGroupsList())
                    {
                        g.map_places(this.network);
                    }
                }

                for (Application a : this.applicationsById.values())
                {
                    for (State s : a.getStates())
                    {
                        s.createPointers(conn);
                    }
                }
                conn.commit();
            }
        }

        // TODO: validate apps
    }

    ///////////////////////////////////////////////////////////////////////////
    // Validation
    ///////////////////////////////////////////////////////////////////////////
    public static Set<ConstraintViolation<Application>> validate(Application a)
    {
        return validatorFactory.getValidator().validate(a);
    }

    public static Set<ConstraintViolation<Network>> validate(Network n)
    {
        return validatorFactory.getValidator().validate(n);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Application loading
    ///////////////////////////////////////////////////////////////////////////
    public Application loadApplication(UUID id, boolean workingCopy, boolean loadNotLocalApps) throws ChronixPlanStorageException
    {
        if (workingCopy)
        {
            Application a = loadApplication(new File(getWorkingPath(id, this.configurationDirectory)), loadNotLocalApps);
            a.isFromCurrentFile(false);
            return a;
        }
        else
        {
            Application a = loadApplication(new File(getActivePath(id, this.configurationDirectory)), loadNotLocalApps);
            a.isFromCurrentFile(true);
            return a;
        }
    }

    private Application loadApplication(File dataFile, boolean loadNotLocalApps) throws ChronixPlanStorageException
    {
        log.info(String.format("Loading an application from file %s", dataFile.getAbsolutePath()));
        Application res = null;

        // Read the XML
        try
        {
            res = (Application) xmlUtility.fromXML(dataFile);
        }
        catch (XStreamException e)
        {
            throw new ChronixPlanStorageException("Could not load file " + dataFile, e);
        }

        // TODO: Don't load applications that are not active on the local node
        //if (CollectionUtils.intersection(this.network.getPlacesIdList(), res.getAllPlacesId()).isEmpty())
        //{
        // log.info(String.format("Application %s has no execution node defined on this server and therefore will not be loaded", res.name));
        // return null;
        //}
        // Set the context so as to enable network access through the application
        res.setContext(this);

        return res;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Network loading
    ///////////////////////////////////////////////////////////////////////////
    public static boolean hasNetworkFile(String configurationDirectory)
    {
        File f = new File(getNetworkPath(new File(configurationDirectory)));
        return f.isFile();
    }

    public Network loadNetwork() throws ChronixPlanStorageException
    {
        File f = new File(getNetworkPath(this.configurationDirectory));
        log.info(String.format("Loading network from file %s", f.getAbsolutePath()));
        Network res = null;

        if (!hasNetworkFile(this.configurationDirectory.getAbsolutePath()))
        {
            throw new ChronixPlanStorageException("Network file " + f.getAbsolutePath() + " does not exist", null);
        }

        try
        {
            res = (Network) xmlUtility.fromXML(f);
        }
        catch (XStreamException e)
        {
            throw new ChronixPlanStorageException("Could not load network file " + f, e);
        }

        return res;
    }

    public void setNetwork(Network n)
    {
        this.network = n;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Save
    ///////////////////////////////////////////////////////////////////////////
    public void saveApplication(Application a) throws ChronixPlanStorageException
    {
        saveApplication(a, this.configurationDirectory);
    }

    /**
     Save the given application to the metabase.
     If the application is already in the metabase, the current version is archived.
     If the application was created from an existing application, the version is incremented before saving.
     @param a
     @param dir
     @throws ChronixPlanStorageException
     */
    public static void saveApplication(Application a, File dir) throws ChronixPlanStorageException
    {
        log.info(String.format("Saving application %s to file inside metabase %s", a.getName(), dir));

        String nextArchiveDataFilePath = dir.getAbsolutePath() + "/app_data_" + a.getId() + "_" + a.getVersion() + "_.crn";
        File nextArchiveDataFile = new File(nextArchiveDataFilePath);
        String currentDataFilePath = getActivePath(a.id, dir);
        File currentData = new File(currentDataFilePath);
        String destPath = getActivePath(a.getId(), dir);

        if (currentData.exists())
        {
            log.info("Current state of application {} will be archived before switching as version {}", a.getName(), a.getVersion());
            if (nextArchiveDataFile.exists())
            {
                log.warn("The version being saved has already been archived once. It won't be re-archived.");
                currentData.delete();
            }
            else if (!currentData.renameTo(nextArchiveDataFile))
            {
                throw new ChronixPlanStorageException("Could not archive current WORKING file", null);
            }
        }

        if (a.isFromCurrentFile())
        {
            a.setVersion(a.getVersion() + 1);
            a.isFromCurrentFile(false);
        }
        log.info("Application {} will be saved as version {} inside file {}", a.getName(), a.getVersion(), destPath);

        try (FileOutputStream fos = new FileOutputStream(destPath))
        {
            xmlUtility.toXML(a, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not save application to file", e);
        }
    }

    public void stageApplication(Application a)
    {
        ChronixContext.stageApplication(a, configurationDirectory);
        this.stagedApplicationsById.put(a.getId(), a);
    }

    private static void stageApplication(Application a, File dir) throws ChronixPlanStorageException
    {
        log.info(String.format("Staging application %s to draft file inside metabase %s", a.getName(), dir));
        String destPath = getWorkingPath(a.getId(), dir);
        a.setLatestSave(DateTime.now());
        
        try (FileOutputStream fos = new FileOutputStream(destPath))
        {
            xmlUtility.toXML(a, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not save application to file", e);
        }
    }

    public void unstageApplication(Application a)
    {
        unstageApplication(a, configurationDirectory);
        this.stagedApplicationsById.remove(a.getId());
    }

    private static void unstageApplication(Application a, File dir)
    {
        String destPath = getWorkingPath(a.getId(), dir);
        File dest = new File(destPath);

        if (dest.exists() && !dest.delete())
        {
            log.info("Removing staging file {} for application {}", destPath, a.getName());
            throw new ChronixPlanStorageException("Could not remove staging file " + destPath, null);
        }
    }

    public void saveNetwork(Network n) throws ChronixPlanStorageException
    {
        saveNetwork(n, this.configurationDirectory);
    }

    public static void saveNetwork(Network n, File dir) throws ChronixPlanStorageException
    {
        log.info(String.format("Saving network to file inside database %s", dir));
        try (FileOutputStream fos = new FileOutputStream(getNetworkPath(dir)))
        {
            xmlUtility.toXML(n, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not save network to file", e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Paths
    ///////////////////////////////////////////////////////////////////////////
    protected static String getWorkingPath(UUID appId, File dir)
    {
        return FilenameUtils.concat(dir.getAbsolutePath(), "app_data_" + appId + "_WORKING_.crn");
    }

    protected static String getActivePath(UUID appId, File dir)
    {
        return FilenameUtils.concat(dir.getAbsolutePath(), "app_data_" + appId + "_CURRENT_.crn");
    }

    protected static String getNetworkPath(File dir)
    {
        return FilenameUtils.concat(dir.getAbsolutePath(), "network_data_CURRENT_.crn");
    }

    protected void preSaveWorkingApp(Application a)
    {
        // TODO: check the app is valid
    }

    public void deleteCurrentApplication(Application a) throws ChronixPlanStorageException
    {
        log.info(String.format("Deleting inside database %s the file for application %s", this.configurationDirectory, a.getName()));
        String currentDataFilePath = getActivePath(a.id, this.configurationDirectory);

        File f = new File(currentDataFilePath);
        f.delete();
    }

    // Will make the transactional data inside the database consistent with the application definition
    public void cleanTransanc()
    {
        try (Connection c = this.getTransacDataSource().beginTransaction())
        {
            // Remove elements from deleted applications
            List<String> quotedIds = new ArrayList<>(); // Hack because no list support in JDBC
            for (UUID u : this.applicationsById.keySet())
            {
                quotedIds.add("'" + u.toString() + "'");
            }
            String appIds = this.applicationsById.size() > 0 ? StringUtils.join(quotedIds, ",") : "'z'";

            c.createQuery("DELETE FROM Event tb WHERE tb.appID NOT IN (" + appIds + ")").executeUpdate();
            c.createQuery("DELETE FROM PipeLineJob tb WHERE tb.appID NOT IN (" + appIds + ")").executeUpdate();
            c.createQuery("DELETE FROM ClockTick ct WHERE ct.appID NOT IN (" + appIds + ")").executeUpdate();
            c.createQuery("DELETE FROM TokenReservation tr WHERE tr.applicationId NOT IN (" + appIds + ")").executeUpdate();

            // Remove wrong transient elements
            c.createQuery("DELETE FROM CalendarPointer WHERE CalendarID IS NULL").executeUpdate();
            c.createQuery("DELETE FROM Event WHERE stateID IS NULL OR placeID IS NULL OR ActiveID IS NULL").executeUpdate();
            c.createQuery("DELETE FROM PipelineJob WHERE stateID IS NULL OR placeID IS NULL OR ActiveID IS NULL").executeUpdate();

            // For each still existing application, do purge transient elements related to removed elements
            for (Application a : this.applicationsByName.values())
            {
                // EVENTS
                Query q1 = c.createQuery("DELETE FROM Event WHERE id=:id");
                Query q2 = c.createQuery("DELETE FROM ENVIRONMENTVALUE WHERE transientid=:id");
                Query q3 = c.createQuery("DELETE FROM EVENTCONSUMPTION WHERE eventid=:id");
                int i1 = 0;
                for (Event e : c.createQuery("SELECT * FROM Event WHERE appID=:a").addParameter("a", a.getId()).executeAndFetch(Event.class))
                {
                    if (e.getState(this) == null || e.getPlace(this) == null || e.getActive(this) == null)
                    {
                        q1.addParameter("id", e.getId()).addToBatch();
                        q2.addParameter("id", e.getId()).addToBatch();
                        q3.addParameter("id", e.getId()).addToBatch();
                        i1++;
                    }
                }
                if (i1 > 0)
                {
                    q1.executeBatch();
                    q2.executeBatch();
                    q3.executeBatch();
                }

                // PJ
                q1 = c.createQuery("DELETE FROM PipelineJob WHERE id=:id");
                i1 = 0;
                for (PipelineJob e : c.createQuery("SELECT * FROM PipelineJob WHERE appID=:a").addParameter("a", a.getId()).executeAndFetch(PipelineJob.class))
                {
                    if (e.getState(this) == null || e.getPlace(this) == null || e.getActive(this) == null)
                    {
                        q1.addParameter("id", e.getId()).addToBatch();
                        q2.addParameter("id", e.getId()).addToBatch();
                        i1++;
                    }
                }
                if (i1 > 0)
                {
                    q1.executeBatch();
                    q2.executeBatch();
                }

                // CALENDARPOINTER
                q1 = c.createQuery("DELETE FROM CALENDARPOINTER WHERE id=:id");
                i1 = 0;
                for (CalendarPointer tb : c.createQuery("SELECT * FROM CalendarPointer WHERE appID=:a").addParameter("a", a.getId()).executeAndFetch(CalendarPointer.class))
                {
                    // CalendarPointer are special: there is one without Place & State per Calendar (the calendar main pointer)
                    tb.getCalendar(this);
                    if (tb.getStateID() != null || tb.getPlaceID() != null)
                    {
                        if (tb.getStateID() == null || tb.getPlaceID() == null)
                        {
                            q1.addParameter("id", tb.getId()).addToBatch();
                        }
                    }
                }
                if (i1 > 0)
                {
                    q1.executeBatch();
                }

                // CT must have a clock
                q1 = c.createQuery("DELETE FROM ClockTick WHERE id=:id");
                i1 = 0;
                for (ClockTick ct : c.createQuery("SELECT * FROM ClockTick ct").executeAndFetch(ClockTick.class))
                {
                    try
                    {
                        ct.getClock(this);
                    }
                    catch (Exception e)
                    {
                        q1.addParameter("id", ct.getId()).addToBatch();
                        i1++;
                    }
                }
                if (i1 > 0)
                {
                    q1.executeBatch();
                }

                // TR must have Place, State, Token
                q1 = c.createQuery("DELETE FROM TokenReservation WHERE id=:id");
                i1 = 0;
                for (TokenReservation tr : c.createQuery("SELECT * FROM TokenReservation WHERE APPLICATIONID=:a").addParameter("a", a.getId()).executeAndFetch(TokenReservation.class))
                {
                    try
                    {
                        tr.getState(this);
                        tr.getPlace(this);
                        tr.getToken(this);
                    }
                    catch (Exception e)
                    {
                        q1.addParameter("id", tr.getId()).addToBatch();
                        i1++;
                    }
                }
                if (i1 > 0)
                {
                    q1.executeBatch();
                }
            }

            c.commit();
        }
    }

    public Network getNetwork()
    {
        return network;
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

    public void removeApplicationFromCache(UUID appID)
    {
        Application a = this.applicationsById.get(appID);
        this.applicationsById.remove(appID);
        this.applicationsByName.remove(a.name);
    }

    public boolean hasLocalConsole()
    {
        ExecutionNode console = this.network.getConsoleNode();
        return localNodeName.equals(console.getName());
    }

    public ExecutionNode getLocalNode()
    {
        if (localNode == null)
        {
            this.localNode = this.network.getNode(this.localNodeName);
        }
        return this.localNode;
    }

    public void setLocalNode(ExecutionNode node)
    {
        this.localNode = node;
    }

    private void setSimulation(boolean simulation)
    {
        this.simulateExternalPayloads = simulation;
    }

    public boolean isSimulator()
    {
        return simulateExternalPayloads;
    }

    public void setSimulator()
    {
        this.simulateExternalPayloads = true;
    }

    public Application getApplication(UUID id)
    {
        return this.applicationsById.get(id);
    }

    public Application getStagedApplication(UUID id)
    {
        return this.stagedApplicationsById.containsKey(id) ? this.stagedApplicationsById.get(id) : this.getApplication(id);
    }

    public Application getApplication(String id)
    {
        return this.getApplication(UUID.fromString(id));
    }

    public Application getApplicationByName(String name)
    {
        return this.applicationsByName.get(name);
    }

    public Collection<Application> getApplications()
    {
        return this.applicationsById.values();
    }

    public Collection<Application> getStagedApplications()
    {
        Map<UUID, Application> res = new HashMap<>();
        res.putAll(this.applicationsById);
        res.putAll(this.stagedApplicationsById);
        return res.values();
    }

    public String getContextRoot()
    {
        return this.configurationDirectory.getAbsolutePath();
    }

    public void addApplicationToCache(Application a)
    {
        this.applicationsById.put(a.getId(), a);
        this.applicationsByName.put(a.getName(), a);
    }

    public DateTime getLoadTime()
    {
        return loaded;
    }

    public void close()
    {
        if (this.historyDS != null)
        {
            try (Connection conn = this.historyDS.open())
            {
                conn.createQuery("SHUTDOWN").executeUpdate();
                log.debug("History database closed");
            }
            catch (Exception e)
            {
                log.warn("Could not close history database on context destruction", e);
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
                log.warn("Could not close transac database on context destruction", e);
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
    }

    public void setLocalNodeName(String name)
    {
        this.localNodeName = name;
    }
}
