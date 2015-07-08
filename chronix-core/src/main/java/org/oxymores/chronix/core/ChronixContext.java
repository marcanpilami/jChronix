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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.ClockTick;
import org.oxymores.chronix.core.transactional.TokenReservation;
import org.oxymores.chronix.core.transactional.TranscientBase;
import org.oxymores.chronix.exceptions.ChronixNoLocalNode;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;

public class ChronixContext
{
    private static final Logger log = Logger.getLogger(ChronixContext.class);
    private static ValidatorFactory validatorFactory;
    private static EntityManagerFactory historyEmf, transacEmf;
    private static final XStream xmlUtility = new XStream(new StaxDriver());

    // Data needed to load the applications
    private DateTime loaded;
    private File configurationDirectory;
    private String transacUnitName, historyUnitName, historyDbPath, transacDbPath;

    // Loaded applications
    private Map<UUID, Application> applicationsById;
    private Map<String, Application> applicationsByName;

    // Local node identification
    private String localUrl = "";
    private String dns;
    private int port;

    // Simulation data
    private boolean simulateExternalPayloads = false;

    static
    {
        xmlUtility.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
    }

    /**
     * Creates a minimal Context with no applications loaded.
     *
     * @param appConfDirectory
     * @param transacUnitName
     * @param historyUnitName
     * @param brokerInterface
     * @param simulation
     * @return
     */
    public static ChronixContext initContext(String appConfDirectory, String transacUnitName, String historyUnitName, String brokerInterface, boolean simulation)
    {
        ChronixContext ctx = new ChronixContext();
        ctx.loaded = DateTime.now();
        ctx.configurationDirectory = new File(FilenameUtils.normalize(appConfDirectory));
        ctx.applicationsById = new HashMap<>();
        ctx.applicationsByName = new HashMap<>();
        ctx.historyUnitName = historyUnitName;
        ctx.transacUnitName = transacUnitName;
        ctx.localUrl = brokerInterface;
        ctx.dns = ctx.localUrl.split(":")[0];
        ctx.port = Integer.parseInt(ctx.localUrl.split(":")[1]);
        ctx.setSimulation(simulation);

        return ctx;
    }

    /**
     * Creates a new Context. This calls initContext, so no need to call it beforehand.
     *
     * @param appConfDirectory
     * @param transacUnitName
     * @param historyUnitName
     * @param brokerInterface
     * @param simulation
     * @param historyDBPath
     * @param transacDbPath
     * @return
     * @throws ChronixPlanStorageException
     */
    public static ChronixContext loadContext(String appConfDirectory, String transacUnitName, String historyUnitName,
            String brokerInterface, boolean simulation, String historyDBPath, String transacDbPath) throws ChronixPlanStorageException
    {
        log.info(String.format("Creating a new context from configuration database %s", appConfDirectory));

        if (!(new File(appConfDirectory).isDirectory()))
        {
            throw new ChronixPlanStorageException("Directory " + appConfDirectory + " does not exist", null);
        }

        ChronixContext ctx = initContext(appConfDirectory, transacUnitName, historyUnitName, brokerInterface, simulation);
        ctx.historyDbPath = historyDBPath;
        ctx.transacDbPath = transacDbPath;

        // List files in directory - and therefore applications
        File[] fileList = ctx.configurationDirectory.listFiles();
        HashMap<String, File[]> toLoad = new HashMap<>();

        for (File f : fileList)
        {
            String fileName = f.getName();

            if (fileName.startsWith("app_") && fileName.endsWith(".crn") && fileName.contains("CURRENT") && fileName.contains("data"))
            {
                // This is a current app DATA file.
                String id = fileName.split("_")[2];
                if (!toLoad.containsKey(id))
                {
                    toLoad.put(id, new File[2]);
                }
                toLoad.get(id)[0] = f;
            }
        }

        // ///////////////////
        // Load apps
        // ///////////////////
        for (File[] ss : toLoad.values())
        {
            ctx.loadApplication(ss[0], simulation);
        }

        // ///////////////////
        // Post load checkup & inits
        // ///////////////////
        if (ctx.applicationsById.values().size() > 0)
        {
            EntityManager em = ctx.getTransacEM();
            EntityTransaction tr = em.getTransaction();
            tr.begin();
            for (Application a : ctx.applicationsById.values())
            {
                for (Calendar c : a.calendars.values())
                {
                    c.createPointers(em);
                }
            }
            tr.commit();
            tr.begin();
            for (Application a : ctx.applicationsById.values())
            {
                for (State s : a.getStates())
                {
                    s.createPointers(em);
                }
            }
            tr.commit();
        }

        // TODO: cleanup event data (elements they reference may have been removed)
        // TODO: validate apps
        // Done!
        return ctx;
    }

    public static Validator getValidator()
    {
        if (validatorFactory == null)
        {
            //Configuration<?> configuration = Validation.byDefaultProvider().configure();
            validatorFactory = Validation.buildDefaultValidatorFactory();
        }
        return validatorFactory.getValidator();
    }

    public static Set<ConstraintViolation<Application>> validate(Application a)
    {
        return getValidator().validate(a);
    }

    public Application loadApplication(UUID id, boolean workingCopy, boolean loadNotLocalApps) throws ChronixPlanStorageException
    {
        if (workingCopy)
        {
            return loadApplication(new File(getWorkingPath(id, this.configurationDirectory)), loadNotLocalApps);
        }
        else
        {
            return loadApplication(new File(getActivePath(id, this.configurationDirectory)), loadNotLocalApps);
        }
    }

    public Application loadApplication(File dataFile, boolean loadNotLocalApps) throws ChronixPlanStorageException
    {
        log.info(String.format("(%s) Loading an application from file %s", this.configurationDirectory, dataFile.getAbsolutePath()));
        Application res = null;

        try
        {
            res = (Application) xmlUtility.fromXML(dataFile);
        }
        catch (XStreamException e)
        {
            throw new ChronixPlanStorageException("Could not load file " + dataFile, e);
        }

        try
        {
            res.setLocalNode(this.dns, this.port);
        }
        catch (ChronixNoLocalNode e)
        {
            // no local node means this application should not run here
            if (!loadNotLocalApps)
            {
                log.info(String.format("Application %s has no execution node defined on this server and therefore will not be loaded", res.name));
                return null;
            }
        }
        applicationsById.put(res.getId(), res);
        applicationsByName.put(res.getName(), res);

        return res;
    }

    public void saveApplication(String name) throws ChronixPlanStorageException
    {
        saveApplication(this.applicationsByName.get(name));
    }

    public void saveApplication(UUID id) throws ChronixPlanStorageException
    {
        saveApplication(this.applicationsById.get(id));
    }

    public void saveApplication(Application a) throws ChronixPlanStorageException
    {
        saveApplication(a, this.configurationDirectory);
    }

    public static void saveApplication(Application a, File dir) throws ChronixPlanStorageException
    {
        log.info(String.format("(%s) Saving application %s to temp file", dir, a.getName()));
        try (FileOutputStream fos = new FileOutputStream(getWorkingPath(a.getId(), dir)))
        {
            xmlUtility.toXML(a, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not save application to temp file", e);
        }
    }

    public static void saveApplicationAndMakeCurrent(Application a, File dir) throws ChronixPlanStorageException
    {
        saveApplication(a, dir);
        setWorkingAsCurrent(a, dir);
    }

    protected static String getWorkingPath(UUID appId, File dir)
    {
        return FilenameUtils.concat(dir.getAbsolutePath(), "app_data_" + appId + "_WORKING_.crn");
    }

    protected static String getActivePath(UUID appId, File dir)
    {
        return FilenameUtils.concat(dir.getAbsolutePath(), "app_data_" + appId + "_CURRENT_.crn");
    }

    protected void preSaveWorkingApp(Application a)
    {
        // TODO: check the app is valid
    }

    // Does NOT refresh caches. Restart the engine for that !
    public void setWorkingAsCurrent(Application a) throws ChronixPlanStorageException
    {
        setWorkingAsCurrent(a, this.configurationDirectory);
    }

    public static void setWorkingAsCurrent(Application a, File dir) throws ChronixPlanStorageException
    {
        log.info(String.format("(%s) Promoting temp file for application %s as the active file", dir, a.getName()));
        String workingDataFilePath = getWorkingPath(a.id, dir);
        String currentDataFilePath = getActivePath(a.id, dir);

        File workingData = new File(workingDataFilePath);
        if (!workingData.exists())
        {
            throw new ChronixPlanStorageException("work file does not exist. You sure 'bout that? You seem to have made no changes!", null);
        }
        File currentData = new File(currentDataFilePath);

        // Get latest version
        File[] fileList = dir.listFiles();
        int v = 0;
        for (File f : fileList)
        {
            String fileName = f.getName();
            if (fileName.startsWith("app_") && fileName.endsWith(".crn") && fileName.contains(a.getId().toString())
                    && !fileName.contains("CURRENT") && !fileName.contains("WORKING") && fileName.contains("data"))
            {
                Integer tmp;
                try
                {
                    tmp = Integer.parseInt(fileName.split("_")[3]);
                }
                catch (NumberFormatException e)
                {
                    tmp = 0;
                }
                if (tmp > v)
                {
                    v = tmp;
                }
            }
        }
        v++;
        log.info(String.format("(%s) Current state of application %s will be saved before switching as version %s", dir, a.getName(), v));

        String nextArchiveDataFilePath = dir.getAbsolutePath() + "/app_data_" + a.getId() + "_" + v + "_.crn";
        File nextArchiveDataFile = new File(nextArchiveDataFilePath);

        // Move CURRENT to the new archive version
        if (currentData.exists() && !currentData.renameTo(nextArchiveDataFile))
        {
            throw new ChronixPlanStorageException("Could not archive current WORKING file", null);
        }

        // Move WORKING as the new CURRENT
        log.debug(String.format("(%s) New path will be %s", dir, currentDataFilePath));
        if (!workingData.renameTo(new File(currentDataFilePath)))
        {
            throw new ChronixPlanStorageException("Could not copy current WORKING file as CURRENT file", null);
        }
    }

    public void deleteCurrentApplication(Application a) throws ChronixPlanStorageException
    {
        log.info(String.format("(%s) Deleting active file for application %s", this.configurationDirectory, a.getName()));
        String currentDataFilePath = getActivePath(a.id, this.configurationDirectory);

        File f = new File(currentDataFilePath);
        f.delete();
        this.removeApplicationFromCache(a.getId());
    }

    // Will make the transactional data inside the database consistent with the application definition
    public void cleanTransanc()
    {
        EntityManager em = this.getTransacEM();
        em.getTransaction().begin();

        // Remove elements from deleted applications
        for (String appId : em.createQuery("SELECT DISTINCT tb.appID FROM TranscientBase tb", String.class).getResultList())
        {
            if (appId != null && this.getApplication(appId) != null)
            {
                continue;
            }
            em.createQuery("DELETE FROM TranscientBase tb WHERE tb.appID = :a").setParameter("a", appId).executeUpdate();
            em.createQuery("DELETE FROM ClockTick ct WHERE ct.appId = :a").setParameter("a", appId).executeUpdate();
            em.createQuery("DELETE FROM TokenReservation tr WHERE tr.applicationId = :a").setParameter("a", appId).executeUpdate();
        }

        // For each application, purge...
        for (Application a : this.applicationsByName.values())
        {
            // TB must have a State, a Place, a Source. Purging TB implies purging EV.
            for (TranscientBase tb : em.createQuery("SELECT tb FROM TranscientBase tb WHERE tb.appID=:a", TranscientBase.class)
                    .setParameter("a", a.getId().toString()).getResultList())
            {
                if (tb instanceof CalendarPointer)
                {
                    // CalendarPointer are special: there is one without Place & State per Calendar (the calendar main pointer)
                    try
                    {
                        tb.getCalendar(this);
                        if (tb.getStateID() != null || tb.getPlaceID() != null || tb.getActiveID() != null)
                        {
                            if (tb.getState(this) == null || tb.getPlace(this) == null)
                            {
                                em.remove(tb);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        em.remove(tb);
                    }
                }
                else
                {
                    // General case
                    try
                    {
                        if (tb.getState(this) == null || tb.getPlace(this) == null || tb.getActive(this) == null)
                        {
                            em.remove(tb);
                        }
                    }
                    catch (Exception e)
                    {
                        em.remove(tb);
                    }
                }
            }

            // CT must have a clock
            for (ClockTick ct : em.createQuery("SELECT ct FROM ClockTick ct WHERE ct.appId=:a", ClockTick.class)
                    .setParameter("a", a.getId().toString()).getResultList())
            {
                try
                {
                    ct.getClock(this);
                }
                catch (Exception e)
                {
                    em.remove(ct);
                }
            }

            // TR must have Place, State, Token
            for (TokenReservation tr : em
                    .createQuery("SELECT tr FROM TokenReservation tr WHERE tr.applicationId=:a", TokenReservation.class)
                    .setParameter("a", a.getId().toString()).getResultList())
            {
                try
                {
                    tr.getState(this);
                    tr.getPlace(this);
                    tr.getToken(this);
                }
                catch (Exception e)
                {
                    em.remove(tr);
                }
            }
        }

        em.getTransaction().commit();
        em.close();
    }

    public Map<UUID, ExecutionNode> getNetwork()
    {
        Map<UUID, ExecutionNode> res = new HashMap<>();
        for (Application a : applicationsById.values())
        {
            res.putAll(a.nodes);
        }
        return res;
    }

    public String getBrokerName()
    {
        return this.localUrl.replace(":", "").toUpperCase();
    }

    public EntityManagerFactory getTransacEMF()
    {
        if (transacEmf != null)
        {
            return transacEmf;
        }
        Properties p = new Properties();
        if (this.transacDbPath != null)
        {
            p.put("openjpa.ConnectionURL", "jdbc:hsqldb:file:" + this.transacDbPath);
        }
        return Persistence.createEntityManagerFactory(this.transacUnitName, p);
    }

    public EntityManagerFactory getHistoryEMF()
    {
        if (historyEmf != null)
        {
            return historyEmf;
        }
        Properties p = new Properties();
        if (this.historyDbPath != null)
        {
            p.put("openjpa.ConnectionURL", "jdbc:hsqldb:file:" + this.historyDbPath);
        }
        return Persistence.createEntityManagerFactory(this.historyUnitName, p);
    }

    public EntityManager getTransacEM()
    {
        return this.getTransacEMF().createEntityManager();
    }

    public EntityManager getHistoryEM()
    {
        return this.getHistoryEMF().createEntityManager();
    }

    public void removeApplicationFromCache(UUID appID)
    {
        Application a = this.applicationsById.get(appID);
        this.applicationsById.remove(appID);
        this.applicationsByName.remove(a.name);
    }

    public boolean hasLocalConsole()
    {
        for (Application a : this.applicationsById.values())
        {
            if (a.getConsoleNode() != null && a.getLocalNode() == a.getConsoleNode())
            {
                return true;
            }
        }
        return false;
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

    public String getContextRoot()
    {
        return this.configurationDirectory.getAbsolutePath();
    }

    public void addApplicationToCache(Application a)
    {
        this.applicationsById.put(a.getId(), a);
        this.applicationsByName.put(a.getName(), a);
    }

    public String getBrokerUrl()
    {
        return this.localUrl;
    }

    public String getLocalBrokerUrl()
    {
        return "vm://" + this.getBrokerName();
    }

    public DateTime getLoadTime()
    {
        return loaded;
    }

    public int getMessagePort()
    {
        return this.port;
    }

    public String getMainInterface()
    {
        return this.dns;
    }
}
