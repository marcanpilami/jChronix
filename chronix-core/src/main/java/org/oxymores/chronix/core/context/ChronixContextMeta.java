package org.oxymores.chronix.core.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
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
    private ServiceTracker<EventSourceProvider, EventSourceProvider> tracker;

    // all known applications
    private Map<UUID, Application2> applications = new HashMap<>();
    private Map<UUID, Application2> drafts = new HashMap<>();

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
        tracker = new ServiceTracker<EventSourceProvider, EventSourceProvider>(bd.getBundleContext(), EventSourceProvider.class,
                new EventSourceTracker(this));
        tracker.open();
        log.debug("Source event plugin tracker is open");
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
            Application2 app = deserializeApp(new File(FilenameUtils.concat(appDir.getAbsolutePath(), "current")), xmlUtility);
            this.applications.put(app.getId(), app);
        }
        for (File appDir : draftDir.listFiles())
        {
            Application2 app = deserializeApp(appDir, xmlUtility);
            this.drafts.put(app.getId(), app);
        }
    }

    private Application2 deserializeApp(File appDir, XStream xmlUtility)
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

        Application2 app;
        try
        {
            app = (Application2) xmlUtility.fromXML(appFile);
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
    public Application2 getApplication(UUID id)
    {
        return this.applications.get(id);
    }

    /**
     * Only active applications
     */
    public Collection<Application2> getApplications()
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
    private File getRootApplicationDraft(UUID id)
    {
        return new File(FilenameUtils.concat(this.getRootApplicationDraft().getAbsolutePath(), id.toString()));
    }

    /**
     * Get an application draft. Null if not found.
     */
    public Application2 getApplicationDraft(UUID id)
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
    public Collection<Application2> getDrafts()
    {
        return this.drafts.values();
    }

    /**
     * This method is called whenever an API user wants to update a draft. All it does is store it to disk and add the draft to the context.
     */
    public void saveApplicationDraft(Application2 app)
    {
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ChronixContextMeta.class.getClassLoader());
        xmlUtility.processAnnotations(Application2.class);

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
        try (FileOutputStream fos = new FileOutputStream(this.getRootApplicationDraft(app.getId()) + "/app.xml"))
        {
            xmlUtility.toXML(app, fos);
        }
        catch (Exception e)
        {
            throw new ChronixPlanStorageException("Could not save application to file", e);
        }

        for (Object ob : this.getAllKnownBehaviours())
        {
            if (ob == null)
            {
                continue;
            }

            for (Class<? extends EventSource> c : app.getAllEventSourceClasses())
            {
                String f = FrameworkUtil.getBundle(c).getSymbolicName();
                File targetBundleDir = new File(FilenameUtils.concat(targetDir.getAbsolutePath(), f));

                if (!targetBundleDir.exists() && !targetBundleDir.mkdir())
                {
                    throw new ChronixPlanStorageException(
                            "plugin directory does not exist and cannot be created: " + targetBundleDir.getAbsolutePath());
                }

                List<? extends EventSource> p = app.getEventSources(c);
                if (p.size() == 0)
                {
                    continue;
                }
                getProviderForClass(p.get(0).getProvider()).serialise(targetBundleDir, p);
            }
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
        Application2 a = this.getApplicationDraft(appId);
        if (a == null)
        {
            throw new ChronixPlanStorageException("No draft for application " + appId);
        }
        a.addVersion(commitComment);
        saveApplicationDraft(a);

        // Move the current app to an archive directory
        Application2 old = getApplication(appId);
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

    public Object[] getAllKnownBehaviours()
    {
        while (tracker.getServices() == null)
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
        return tracker.getServices();
    }

    public EventSourceProvider getProviderForClass(Class<? extends EventSourceProvider> klass)
    {
        for (Object o : getAllKnownBehaviours())
        {
            EventSourceProvider pr = (EventSourceProvider) o;
            if (pr.getClass().isAssignableFrom(klass))
            {
                return pr;
            }
        }
        throw new ChronixException("no such provider");
    }
}
