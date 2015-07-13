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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.MaintenanceApplication;
import org.oxymores.chronix.planbuilder.OperationsApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;

/**
 * A Chronix Node. Can be either engine + runner or simply runner.
 *
 */
public class ChronixEngine extends Thread
{
    private static Logger log = Logger.getLogger(ChronixEngine.class);

    private boolean runnerMode;
    private int runnerPort, feederPort;
    private String runnerHost, feederHost;

    protected ChronixContext ctx;
    protected String dbPath;
    protected String transacUnitName, historyUnitName, historyDbPath, transacDbPath;
    protected String localNodeName;

    protected Broker broker;
    protected SelfTriggerAgent stAgent;

    protected Semaphore startCritical, stop, threadInit, stopped, stopping;
    protected boolean run = true;
    protected int nbRunner;

    // ///////////////////////////////////////////////////////////////
    // Construction
    public ChronixEngine(String dbPath, String nodeName)
    {
        this(dbPath, nodeName, "TransacUnit", "HistoryUnit");
    }

    public ChronixEngine(String dbPath, String nodeName, String transacUnitName, String historyUnitName)
    {
        this(dbPath, nodeName, transacUnitName, historyUnitName, false, 1);
    }

    public ChronixEngine(String dbPath, String nodeName, String transacUnitName, String historyUnitName, boolean runnerMode)
    {
        this(dbPath, nodeName, transacUnitName, historyUnitName, runnerMode, 1);
    }

    public ChronixEngine(String dbPath, String nodeName, String transacUnitName, String historyUnitName, boolean runnerMode, int nbRunner)
    {
        this(dbPath, nodeName, transacUnitName, historyUnitName, runnerMode, nbRunner, null, null);
    }

    public ChronixEngine(String dbPath, String nodeName, String transacUnitName, String historyUnitName, boolean runnerMode, int nbRunner, String historyDBPath,
            String transacDbPath)
    {
        this.dbPath = dbPath;
        this.runnerMode = runnerMode;
        this.transacUnitName = transacUnitName;
        this.historyUnitName = historyUnitName;
        this.localNodeName = nodeName;
        this.nbRunner = nbRunner;
        this.historyDbPath = historyDBPath;
        this.transacDbPath = transacDbPath;

        // To allow some basic configuration before starting nodes, we init the minimal fields inside the context
        this.ctx = ChronixContext.initContext(dbPath, transacUnitName, historyUnitName, false);

        // Startup phase is synchronized with this
        this.startCritical = new Semaphore(1);
        // Putting a token in this will stop the engine
        this.stop = new Semaphore(0);
        this.threadInit = new Semaphore(0);
        // A token is created when the engines stops. (not when it reboots)
        this.stopped = new Semaphore(0);
        // The stop & start sequence is protected with this
        this.stopping = new Semaphore(1);
    }

    //
    // ///////////////////////////////////////////////////////////////
    protected void startEngine(boolean blocking, boolean purgeQueues) throws Exception
    {
        log.info(String.format("Engine %s starting on database %s", this.localNodeName, this.dbPath));
        this.startCritical.acquire();
        this.threadInit.release(1);

        // Remote nodes must fetch the network on startup if not already present
        if (!runnerMode && feederHost != null && !ctx.hasNetworkFile())
        {
            BootstrapListener bl = new BootstrapListener(ctx, localNodeName, feederHost, feederPort);
            if (!bl.fetchNetwork())
            {
                throw new ChronixInitializationException("Could not fetch network from remote node. Will not be able to start. See errors above for details.");
            }
        }

        // Context
        if (!runnerMode)
        {
            preContextLoad();
            this.ctx = ChronixContext.loadContext(this.dbPath, this.transacUnitName, this.historyUnitName, this.localNodeName, false, this.historyDbPath, this.transacDbPath);
            postContextLoad();
        }

        // Mock-up for hosted nodes
        if (runnerMode)
        {
            ExecutionNode e = new ExecutionNode();
            e.setConsole(false);
            e.setDns(this.runnerHost);
            e.setqPort(this.runnerPort);
            e.setName(this.localNodeName);
            this.ctx.setLocalNode(e);
        }

        // Broker with all the consumer threads
        if (this.broker == null)
        {
            this.broker = new Broker(this.ctx, purgeQueues, !this.runnerMode, true);
        }
        else
        {
            this.broker.resetContext(ctx);
        }
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
        log.info("Engine has finished its boot sequence");

        if (blocking)
        {
            stop.acquire();
        }
    }

    @Override
    public void run()
    {
        MDC.put("node", this.localNodeName);

        // Only does one thing: reload configuration and stop on trigger
        while (this.run)
        {
            // First : start!
            try
            {
                startEngine(false, false);
            }
            catch (Exception e)
            {
                log.fatal("The engine has failed to start", e);
                this.run = false;
                this.startCritical.release();
                this.stop.release();
                this.threadInit.release();
                return;
            }

            // Then, wait for the end signal
            try
            {
                this.stopping.acquire();
                this.stop.acquire();
            }
            catch (InterruptedException e)
            {
                log.error("big problem here", e);
            }
            log.debug(this.localNodeName + " has actually begun to stop");

            // Properly stop the engine
            if (this.stAgent != null)
            {
                this.stAgent.stopAgent();
            }
            this.broker.stopEngineListeners();

            // TCP port release is not immediate, sad.
            try
            {
                Thread.sleep(Constants.BROKER_PORT_FREEING_MS);
            }
            catch (InterruptedException e)
            {
                log.info("Interruption while waiting for port freeing");
            }

            this.stopping.release(1);
            // Done. If 'run' is still true, will restart the engine
        }

        // Stop every thread, not only the event engine threads.
        this.broker.stopRunnerAgents();
        this.broker.stopBroker();
        this.stopped.release();
        this.ctx.close();
        log.info("The scheduler has stopped");
    }

    /**
     * Wait for a reboot to occur & end. If no reboot happens, this function blocks for ever, so it's a little dangerous.
     */
    public void waitForRebootEnd()
    {
        try
        {
            this.stopping.acquire();
        }
        catch (InterruptedException e)
        {
            log.warn("Interruption while waiting for engine to reboot");
        }
        this.stopping.release();
        waitForInitEnd();
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
        log.info(this.localNodeName + " main engine has received a stop request");
        try
        {
            this.startCritical.acquire();
            threadInit.acquire();
            this.run = false;
            log.debug(this.localNodeName + " stop sequence starts now");
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
        if (this.ctx.getApplications().size() == 0 && !this.runnerMode)
        {
            try
            {
                // Create OPERATIONS application
                Application a = OperationsApplication.getNewApplication(this.ctx);
                this.ctx.saveApplication(a);
                this.ctx.setWorkingAsCurrent(a);

                // Create CHRONIX_MAINTENANCE application
                a = MaintenanceApplication.getNewApplication(this.ctx);
                this.ctx.saveApplication(a);
                this.ctx.setWorkingAsCurrent(a);

                // Reload context to load new applications
                this.ctx = ChronixContext.loadContext(this.dbPath, this.transacUnitName, this.historyUnitName, this.localNodeName, false, this.historyDbPath, this.transacDbPath);
            }
            catch (ChronixPlanStorageException e)
            {
                throw new ChronixInitializationException("Could not create default applications", e);
            }
        }

        // Cleanup
        if (!this.runnerMode)
        {
            this.ctx.cleanTransanc();
        }
    }

    public void emptyDb()
    {
        emptyDb(this.dbPath);
    }

    public static void emptyDb(String dbPath)
    {
        // Clear test db directory
        File[] fileList = new File(dbPath).listFiles();
        if (fileList == null)
        {
            // No directory! Nothing to empty...
            return;
        }
        for (int i = 0; i < fileList.length; i++)
        {
            if (!FileUtils.deleteQuietly(fileList[i]))
            {
                log.error("Purge has failed for directory " + fileList[i].getAbsolutePath());
            }
        }
    }

    boolean shouldRun()
    {
        return this.run;
    }

    public ChronixContext getContext()
    {
        return this.ctx;
    }

    public void setRunnerPort(int port)
    {
        this.runnerPort = port;
    }

    public void setRunnerHost(String host)
    {
        this.runnerHost = host;
    }

    public void setFeeder(String host, int port)
    {
        this.feederHost = host;
        this.feederPort = port;
    }

    public void setFeeder(ExecutionNode en)
    {
        this.setFeeder(en.getHost().getDns(), en.getqPort());
    }
}
