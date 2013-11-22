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

package org.oxymores.chronix.engine;

import java.io.File;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.MaintenanceApplication;
import org.oxymores.chronix.planbuilder.OperationsApplication;

/**
 * A Chronix Node. Can be either engine + runner or simply runner.
 * 
 */
public class ChronixEngine extends Thread
{
    private static Logger log = Logger.getLogger(ChronixEngine.class);

    private boolean runnerMode;

    public ChronixContext ctx;
    private String dbPath;
    private String brokerInterface, transacUnitName, historyUnitName;
    private int brokerPort;
    private String brokerInterfaceNoPort;

    private Broker broker;
    private SelfTriggerAgent stAgent;

    private Semaphore startCritical, stop, threadInit, stopped;
    private boolean run = true;
    private int nbRunner;

    // ///////////////////////////////////////////////////////////////
    // Construction
    public ChronixEngine(String dbPath, String mainInterface)
    {
        this(dbPath, mainInterface, "TransacUnit", "HistoryUnit");
    }

    public ChronixEngine(String dbPath, String mainInterface, String transacUnitName, String historyUnitName)
    {
        this(dbPath, mainInterface, transacUnitName, historyUnitName, false, 1);
    }

    public ChronixEngine(String dbPath, String mainInterface, String transacUnitName, String historyUnitName, boolean runnerMode)
    {
        this(dbPath, mainInterface, transacUnitName, historyUnitName, runnerMode, 1);
    }

    public ChronixEngine(String dbPath, String mainInterface, String transacUnitName, String historyUnitName, boolean runnerMode,
            int nbRunner)
    {
        this.dbPath = dbPath;
        this.runnerMode = runnerMode;
        this.transacUnitName = transacUnitName;
        this.historyUnitName = historyUnitName;
        this.brokerInterface = mainInterface;
        this.brokerInterfaceNoPort = this.brokerInterface.split(":")[0];
        this.brokerPort = Integer.parseInt(this.brokerInterface.split(":")[1]);
        this.nbRunner = nbRunner;

        // To allow some basic configuration before starting nodes, we init the minimal fields inside the context
        this.ctx = new ChronixContext();
        this.ctx.configurationDirectoryPath = dbPath;
        this.ctx.configurationDirectory = new File(dbPath);
        this.ctx.localUrl = mainInterface;
        this.ctx.transacUnitName = transacUnitName;
        this.ctx.historyUnitName = historyUnitName;

        this.startCritical = new Semaphore(1);
        this.stop = new Semaphore(0);
        this.threadInit = new Semaphore(0);
        this.stopped = new Semaphore(0);
    }

    //
    // ///////////////////////////////////////////////////////////////

    private void startEngine(boolean blocking, boolean purgeQueues)
    {
        log.info(String.format("(%s) engine starting (%s)", this.dbPath, this));
        try
        {
            this.startCritical.acquire();
            this.threadInit.release(1);

            // Context
            preContextLoad();
            this.ctx = ChronixContext.loadContext(this.dbPath, this.transacUnitName, this.historyUnitName, this.brokerInterface);
            postContextLoad();

            // Broker with all the consumer threads
            this.broker = new Broker(this, purgeQueues);
            this.broker.setNbRunners(this.nbRunner);
            if (!runnerMode)
            {
                this.broker.registerListeners(this);
            }
            else
            {
                this.broker.registerListeners(this, false, true, false, false, false, false, false, false, false);
            }

            // Active sources agent
            if (!this.runnerMode && broker.getEmf() != null)
            {
                this.stAgent = new SelfTriggerAgent();
                this.stAgent.startAgent(broker.getEmf(), ctx, broker.getConnection());
            }

            // Done
            this.startCritical.release();

            if (blocking)
            {
                stop.acquire();
            }

        }
        catch (Exception e)
        {
            log.fatal("The engine has failed to start", e);
            this.run = false;
        }
    }

    @Override
    public void run()
    {
        // Only does one thing: reload configuration and stop on trigger
        while (this.run)
        {
            // First : start!
            startEngine(false, false);
            if (!run)
            {
                this.startCritical.release();
                this.stop.release();
                this.threadInit.release();
                return;
            }

            // Then, wait for the end signal
            try
            {
                this.stop.acquire();
            }
            catch (InterruptedException e)
            {
                log.error("big problem here", e);
            }

            // Properly stop the engine
            if (this.stAgent != null)
            {
                this.stAgent.stopAgent();
            }
            this.broker.stop();

            // TCP port release is not immediate, sad.
            try
            {
                Thread.sleep(Constants.BROKER_PORT_FREEING_MS);
            }
            catch (InterruptedException e)
            {
                log.info("Interruption while waiting for port freeing");
            }

            // Done. If 'run' is still true, will restart the engine
        }
        this.stopped.release();
        log.info("The scheduler has stopped");
    }

    public void waitForInitEnd()
    {
        try
        {
            this.threadInit.acquire();
            this.startCritical.acquire();
        }
        catch (InterruptedException e)
        {
            log.warn("Interruption while waiting for engine to start");
        }
        this.startCritical.release();
        this.threadInit.release();
    }

    public void waitForStopEnd()
    {
        try
        {
            this.stopped.acquire();
        }
        catch (InterruptedException e)
        {
            log.info("Interruption while waiting for engine to stop");
        }
        this.stop.release();
    }

    public void queueReloadConfiguration()
    {
        try
        {
            this.startCritical.acquire();
            threadInit.acquire();
            this.run = true;
            this.stop.release();
        }
        catch (InterruptedException e)
        {
            log.warn("Interruption while waiting for engine to restart");
        }
        this.startCritical.release();
    }

    public void stopEngine()
    {
        log.info("The main engine has received a stop request");
        try
        {
            this.startCritical.acquire();
            this.run = false;
            this.stop.release();
        }
        catch (InterruptedException e)
        {
            log.warn("Interruption while trying to stop the engine");
        }
        this.startCritical.release();
    }

    protected void preContextLoad() throws ChronixInitializationException
    {
        // Nothing yet. Probably never. For overloads?
    }

    protected void postContextLoad() throws ChronixInitializationException
    {
        // First start?
        if (this.ctx.applicationsById.values().size() == 0 && !this.runnerMode)
        {
            try
            {
                // Create OPERATIONS application
                Application a = OperationsApplication.getNewApplication(this.brokerInterfaceNoPort, this.brokerPort);
                this.ctx.saveApplication(a);
                this.ctx.setWorkingAsCurrent(a);

                // Create CHRONIX_MAINTENANCE application
                a = MaintenanceApplication.getNewApplication(this.brokerInterfaceNoPort, this.brokerPort);
                this.ctx.saveApplication(a);
                this.ctx.setWorkingAsCurrent(a);

                // Reload context to load new applications
                this.ctx = ChronixContext.loadContext(this.dbPath, this.transacUnitName, this.historyUnitName, this.brokerInterface);
            }
            catch (ChronixPlanStorageException e)
            {
                throw new ChronixInitializationException("Could not create default applications", e);
            }
        }
    }

    public void emptyDb()
    {
        // Clear test db directory
        File[] fileList = new File(this.dbPath).listFiles();
        for (int i = 0; i < fileList.length; i++)
        {
            if (!fileList[i].delete())
            {
                log.error("Purge has failed for directory " + fileList[i].getAbsolutePath());
            }
        }
    }

    boolean shouldRun()
    {
        return this.run;
    }
}
