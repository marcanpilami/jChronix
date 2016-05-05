package org.oxymores.chronix.core.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.PlaceGroup;
import org.oxymores.chronix.exceptions.ChronixException;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * This class deals with loading and persisting metadata (applications & environment).<br>
 * It first loads application objects, then registers a plugin listener. The plugins will then fill the applications with sources.<br>
 * The base application object is serialized as a single XML file called rootDir/application/UUID/app.xml (where UUID is the app's ID) file.
 */
public class ChronixContextMeta
{
    private static final Logger log = LoggerFactory.getLogger(ChronixContextMeta.class);

    // Persistence roots/datasources
    private File rootDir;

    // Source tracker
    private ServiceTracker<EventSourceProvider, EventSourceProvider> trackerES;
    private ServiceTracker<ParameterProvider, ParameterProvider> trackerPRM;

    // all known applications
    private Map<UUID, Application> applications = new HashMap<>();
    private Map<UUID, Application> drafts = new HashMap<>();

    // The one and only environment
    private Environment envt, envtDraft;

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION & INIT
    ///////////////////////////////////////////////////////////////////////////

    public ChronixContextMeta(String metaRoot)
    {
        if (metaRoot == null)
        {
            throw new IllegalArgumentException("metaroot cannot be null");
        }
        this.rootDir = new File(FilenameUtils.normalize(metaRoot));
        if (!rootDir.isDirectory() && !rootDir.mkdir())
        {
            throw new ChronixInitializationException(
                    "Configuration directory " + rootDir.getAbsolutePath() + " does not exist and could not be created");
        }

        loadEnvironment();
        loadApplications();
        registerTracker();
    }

    private void registerTracker()
    {
        log.debug("Registering source event plugin tracker");
        Bundle bd = FrameworkUtil.getBundle(ChronixContextMeta.class);
        trackerES = new ServiceTracker<EventSourceProvider, EventSourceProvider>(bd.getBundleContext(), EventSourceProvider.class,
                new EventSourceTracker(this));
        trackerES.open();
        log.debug("Source event plugin tracker is open");

        log.debug("Registering parameter plugin tracker");
        trackerPRM = new ServiceTracker<ParameterProvider, ParameterProvider>(bd.getBundleContext(), ParameterProvider.class,
                new ParameterTracker(this));
        trackerPRM.open();
        log.debug("Parameter plugin tracker is open");
    }

    ///////////////////////////////////////////////////////////////////////////
    // LOADING APPS & DRAFTS
    ///////////////////////////////////////////////////////////////////////////

    private void loadApplications()
    {
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ChronixContextMeta.class.getClassLoader());

        File appsDir = this.getRootApplication();
        if (!appsDir.isDirectory() && !appsDir.mkdir())
        {
            throw new ChronixInitializationException(
                    "Configuration directory " + appsDir.getAbsolutePath() + " does not exist and could not be created");
        }
        File draftDir = this.getRootApplicationDraft();
        if (!draftDir.isDirectory() && !draftDir.mkdir())
        {
            throw new ChronixInitializationException(
                    "Configuration directory " + draftDir.getAbsolutePath() + " does not exist and could not be created");
        }

        for (File appDir : appsDir.listFiles())
        {
            Application app = deserializeApp(new File(FilenameUtils.concat(appDir.getAbsolutePath(), "current")), xmlUtility);
            this.applications.put(app.getId(), app);
        }
        for (File appDir : draftDir.listFiles())
        {
            Application app = deserializeApp(appDir, xmlUtility);
            this.drafts.put(app.getId(), app);
        }
    }

    private Application deserializeApp(File appDir, XStream xmlUtility)
    {
        if (!appDir.isDirectory())
        {
            throw new ChronixInitializationException("Not a directory: " + appDir.getAbsolutePath());
        }
        if (!appDir.canExecute())
        {
            throw new ChronixInitializationException("Cannot access directory " + appDir.getAbsolutePath());
        }
        File appFile = new File(FilenameUtils.concat(appDir.getAbsolutePath(), "app.xml"));
        if (!appFile.exists() || !appFile.canRead() || !appFile.canWrite())
        {
            throw new ChronixInitializationException("Cannot access file in RW mode " + appFile.getAbsolutePath());
        }

        Application app;
        try
        {
            xmlUtility.setClassLoader(this.getClass().getClassLoader());
            app = (Application) xmlUtility.fromXML(appFile);
        }
        catch (XStreamException e)
        {
            throw new ChronixInitializationException(
                    "Application XML file cannot be read as an Application object: " + appFile.getAbsolutePath(), e);
        }

        // Helper denormalisation of places (link between envt and app)
        for (PlaceGroup pg : app.getGroupsList())
        {
            pg.map_places(envt);
        }

        // Check all plugins are present
        app.waitForAllPlugins();

        // TODO: Metadata upgrade
        /*
         * DTOApplication dto = app.getDto(); for (EventSourceProvider service : this.getAllKnownSourceProviders()) { for (DTOEve) }
         */

        // TODO: cleanup.

        // Done
        log.info("Application " + app.getName() + " has been deserialized from file " + appDir);
        return app;
    }

    /**
     * The directory containing all metadata
     */
    public File getRootMeta()
    {
        return this.rootDir;
    }

    ///////////////////////////////////////////////////////////////////////////
    // RUNNING APPLICATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The directory containing all the application directories
     */
    private File getRootApplication()
    {
        return new File(FilenameUtils.concat(this.getRootMeta().getAbsolutePath(), "application"));
    }

    /**
     * The directory containing all the metadata pertaining to a specific active application
     */
    public File getRootApplication(UUID id)
    {
        return new File(FilenameUtils.concat(this.getRootApplication().getAbsolutePath(), FilenameUtils.concat(id.toString(), "current")));
    }

    /**
     * Get an archived version of an application
     */
    private File getRootApplication(UUID id, Integer version)
    {
        return new File(
                FilenameUtils.concat(this.getRootApplication().getAbsolutePath(), FilenameUtils.concat(id.toString(), version.toString())));
    }

    /**
     * Get an active application (not a draft). Null if not found.
     */
    public Application getApplication(UUID id)
    {
        return this.applications.get(id);
    }

    /**
     * Only active applications
     */
    public Collection<Application> getApplications()
    {
        return this.applications.values();
    }

    ///////////////////////////////////////////////////////////////////////////
    // DRAFTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The directory containing all the application draft directories
     */
    private File getRootApplicationDraft()
    {
        return new File(FilenameUtils.concat(this.getRootMeta().getAbsolutePath(), "appdraft"));
    }

    /**
     * The directory containing all the metadata pertaining to a specific active application
     */
    public File getRootApplicationDraft(UUID id)
    {
        return new File(FilenameUtils.concat(this.getRootApplicationDraft().getAbsolutePath(), id.toString()));
    }

    /**
     * Get an application draft. Null if not found.
     */
    public Application getApplicationDraft(UUID id)
    {
        return this.drafts.get(id);
    }

    public void resetApplicationDraft(UUID id)
    {
        File f = getRootApplicationDraft(id);
        if (f.isDirectory())
        {
            try
            {
                FileUtils.deleteDirectory(f);
                log.info("Draft for application " + id + " has been destroyed");
            }
            catch (IOException e)
            {
                throw new ChronixException("Could notr emove applicaiton draft", e);
            }
        }
    }

    /**
     * Only draft applications, not active ones
     */
    public Collection<Application> getDrafts()
    {
        return this.drafts.values();
    }

    /**
     * This method is called whenever an API user wants to update a draft. All it does is store it to disk and add the draft to the context.
     */
    public void saveApplicationDraft(Application app)
    {
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ChronixContextMeta.class.getClassLoader());
        xmlUtility.processAnnotations(Application.class);

        if (app.getId() == null)
        {
            app.setId(UUID.randomUUID());
        }
        app.setLatestSave(DateTime.now());

        File targetDir = this.getRootApplicationDraft(app.getId());
        if (!targetDir.isDirectory() && !targetDir.mkdir())
        {
            throw new ChronixPlanStorageException("directory does not exist and cannot be created: " + targetDir.getAbsolutePath());
        }

        log.info("Saving application draft to disk " + app.getId() + " inside " + this.getRootApplicationDraft(app.getId()));

        // Save (directly) the application
        try (FileOutputStream fos = new FileOutputStream(this.getRootApplicationDraft(app.getId()) + "/app.xml"))
        {
            xmlUtility.toXML(app, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not save application to file", e);
        }

        // Make the draft available
        this.drafts.put(app.getId(), app);
    }

    /**
     * Activates the latest saved version of an application draft. Note this actually updates the draft with the new version data.
     * 
     * @param appId
     * @param commitComment
     */
    public void activateApplicationDraft(UUID appId, String commitComment)
    {
        log.info("Activating a draft");
        Application a = this.getApplicationDraft(appId);
        if (a == null)
        {
            throw new ChronixPlanStorageException("No draft for application " + appId);
        }
        a.addVersion(commitComment);
        saveApplicationDraft(a);

        // Move the current app to an archive directory
        Application old = getApplication(appId);
        File activeDir = getRootApplication(appId);
        if (old != null)
        {
            File archiveDir = getRootApplication(appId, old.getVersion());

            // we can receive the same version multiple times in case of resync so clean if needed
            if (archiveDir.isDirectory())
            {
                try
                {
                    FileUtils.deleteDirectory(archiveDir);
                }
                catch (IOException e)
                {
                    throw new ChronixPlanStorageException("Could not create archive for old version of application " + appId, e);
                }
            }

            // Move to archive
            activeDir.renameTo(archiveDir);
        }

        // Copy draft to active
        try
        {
            FileUtils.copyDirectory(getRootApplicationDraft(appId), activeDir);
        }
        catch (IOException e)
        {
            throw new ChronixPlanStorageException("Could not move draft to active location for application " + appId, e);
        }

        // Make the app available
        this.applications.put(appId, a);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ENVIRONMENT
    ///////////////////////////////////////////////////////////////////////////

    private File getFileEnvironment()
    {
        return new File(FilenameUtils.concat(this.getRootMeta().getAbsolutePath(), "environment/envt.xml"));
    }

    private File getFileEnvironmentDraft()
    {
        return new File(FilenameUtils.concat(this.getRootMeta().getAbsolutePath(), "envtdraft/envt.xml"));
    }

    private void loadEnvironment()
    {
        this.envt = deserializeEnvironment(getFileEnvironment());
        this.envtDraft = deserializeEnvironment(getFileEnvironmentDraft());
    }

    private Environment deserializeEnvironment(File envtFile)
    {
        if (!envtFile.getParentFile().isDirectory() && !envtFile.getParentFile().mkdir())
        {
            throw new ChronixPlanStorageException(
                    "directory " + envtFile.getParentFile().getAbsolutePath() + " does not exist and cannot be created");
        }

        if (!envtFile.exists())
        {
            return null; // no environment or draft for now
        }
        if (!envtFile.canRead() || !envtFile.canWrite())
        {
            throw new ChronixPlanStorageException("Cannot access file " + envtFile.getAbsolutePath() + " in R/W mode");
        }

        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ChronixContextMeta.class.getClassLoader());

        try
        {
            return (Environment) xmlUtility.fromXML(envtFile);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException(
                    "Could not read file " + envtFile.getAbsolutePath() + " as an environment. Probable file corruption.", e);
        }
    }

    public void saveEnvironmentDraft(Environment envt)
    {
        File target = this.getFileEnvironmentDraft();
        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdir())
        {
            throw new ChronixPlanStorageException(
                    "directory " + target.getParentFile().getAbsolutePath() + " does not exist and cannot be created");
        }
        log.info("Saving environment draft to disk inside " + target.getAbsolutePath());

        XStream xmlUtility = new XStream(new StaxDriver());

        try (FileOutputStream fos = new FileOutputStream(target))
        {
            xmlUtility.toXML(envt, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not store environment draft", e);
        }
        this.envtDraft = envt;
    }

    public Environment getEnvironment()
    {
        return this.envt;
    }

    public Environment getEnvironmentDraft()
    {
        return this.envtDraft;
    }

    public void activateEnvironmentDraft(String commitComment)
    {
        log.info("Activating environment draft");
        if (this.envtDraft == null)
        {
            throw new ChronixPlanStorageException("No draft for environment");
        }

        // Copy draft to active
        try
        {
            FileUtils.copyFileToDirectory(this.getFileEnvironmentDraft(), this.getFileEnvironment().getParentFile());
        }
        catch (IOException e)
        {
            throw new ChronixPlanStorageException("Could not move draft to active location for environment", e);
        }

        // Make the envt available
        this.envt = this.envtDraft;
    }

    ///////////////////////////////////////////////////////////////////////////
    // MISC GET/SET
    ///////////////////////////////////////////////////////////////////////////

    public List<EventSourceProvider> getAllKnownSourceProviders()
    {
        while (trackerES.getServices() == null)
        {
            log.info("No known event sources! Engine will wait for a plugin to register and retry");
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // Nothing to do
            }
        }
        List<EventSourceProvider> srvs = new ArrayList<>();
        for (Object o : trackerES.getServices())
        {
            srvs.add((EventSourceProvider) o);
        }
        return srvs;
    }

    private EventSourceProvider getSourceProviderForClass(Class<? extends EventSourceProvider> klass)
    {
        for (EventSourceProvider pr : getAllKnownSourceProviders())
        {
            if (pr.getClass().isAssignableFrom(klass))
            {
                return pr;
            }
        }
        throw new ChronixException("no such provider");
    }

    /**
     * Throws an exception if nothing found. Does not wait for a provider.
     * 
     * @param className
     *            - canonical class name
     * @return
     */
    public EventSourceProvider getSourceProvider(String className)
    {
        for (EventSourceProvider res : this.getAllKnownSourceProviders())
        {
            if (res.getClass().getCanonicalName().equals(className))
            {
                return res;
            }
        }
        throw new ChronixPlanStorageException("no provider of class " + className);
    }

    private Object[] getAllKnownParameterProviders()
    {
        while (trackerPRM.getServices() == null)
        {
            log.info("No known parameter plugins! Engine will wait for a plugin to register and retry");
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // Nothing to do
            }
        }
        return trackerPRM.getServices();
    }

    private ParameterProvider getParameterProviderForClass(Class<? extends ParameterProvider> klass)
    {
        for (Object o : getAllKnownParameterProviders())
        {
            ParameterProvider pr = (ParameterProvider) o;
            if (pr.getClass().isAssignableFrom(klass))
            {
                return pr;
            }
        }
        throw new ChronixException("no such provider");
    }

    public ParameterProvider getParameterProvider(String className)
    {
        for (Object o : this.getAllKnownParameterProviders())
        {
            ParameterProvider res = (ParameterProvider) o;
            if (res.getClass().getCanonicalName().equals(className))
            {
                return res;
            }
        }
        throw new ChronixPlanStorageException("no provider of class " + className);
    }
}
