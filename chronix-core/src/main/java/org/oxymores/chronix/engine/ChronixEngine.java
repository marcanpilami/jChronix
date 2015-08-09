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

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.engine.helpers.ContextHelper;
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

    private boolean runnerMode, cleanStart = false;
    private int runnerPort, feederPort;
    private String runnerHost, feederHost;

    protected ChronixContext ctx;
    protected String dbPath;
    protected String transacUnitName, historyUnitName, historyDbPath, transacDbPath;
    protected String localNodeName;

    protected Broker broker;
    protected SelfTriggerAgent stAgent;

    protected Semaphore startCritical, stop, startSequenceOngoing, stopped, engineStops, engineStarts;
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
        this(dbPath, nodeName, transacUnitName, historyUnitName, runnerMode, nbRunner, FilenameUtils.concat(dbPath, "db_history/db"), FilenameUtils.concat(dbPath, "db_transac/db"));
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

        // Startup phase is synchronized with this
        this.startCritical = new Semaphore(1);
        // Putting a token in this will stop the engine
        this.stop = new Semaphore(0);
        this.startSequenceOngoing = new Semaphore(0);
        // A token is created when the engines stops. (not when it reboots)
        this.stopped = new Semaphore(0);
        // The stop & start sequence is protected with this
        this.engineStops = new Semaphore(0);
        // Will receive one token after each successful or failed start
        this.engineStarts = new Semaphore(0);
    }

    //
    // ///////////////////////////////////////////////////////////////
    protected void startEngine(boolean blocking) throws Exception
    {
        log.info(String.format("Engine %s starting on database %s", this.localNodeName, this.dbPath));
        this.startCritical.acquire();
        this.startSequenceOngoing.release(1);

        // Remote nodes must fetch the network on startup if not already present
        if (!runnerMode && feederHost != null && !ctx.hasNetworkFile(this.dbPath))
        {
            BootstrapListener bl = new BootstrapListener(new File(this.dbPath), localNodeName, feederHost, feederPort);
            if (!bl.fetchNetwork())
            {
                throw new ChronixInitializationException("Could not fetch network from remote node. Will not be able to start. See errors above for details.");
            }
        }

        // Context
        if (!runnerMode)
        {
            preContextLoad();
            this.ctx = new ChronixContext(this.localNodeName, this.dbPath, this.transacUnitName, this.historyUnitName, false, this.historyDbPath, this.transacDbPath);
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
            this.ctx = new ChronixContext("runner", this.dbPath, null, null, false, null, null);
            this.ctx.setLocalNode(e);
        }

        // Broker with all the consumer threads
        if (this.broker == null)
        {
            this.broker = new Broker(this.ctx, cleanStart, !this.runnerMode, true);
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
        if (!this.runnerMode)
        {
            this.stAgent = new SelfTriggerAgent();
            this.stAgent.startAgent(ctx, broker.getConnection());
        }

        // Done
        this.startCritical.release();
        this.startSequenceOngoing.acquire();
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
                startEngine(false);
            }
            catch (Exception e)
            {
                log.fatal("The engine has failed to start", e);
                this.run = false;
                this.startCritical.release();
                this.stop.release();
                this.startSequenceOngoing.release();
                return;
            }
            finally
            {
                this.engineStarts.release();
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

            this.engineStops.release(1);
            // Done. If 'run' is still true, will restart the engine
        }

        // Stop every thread, not only the event engine threads.
        this.broker.stopRunnerAgents();
        this.broker.stopBroker();
        this.ctx.close();
        this.stopped.release();
        log.info("The scheduler has stopped");
    }

    /**
     * Wait for a reboot to occur & end. If no reboot happens, this function blocks for ever, so it's a little dangerous.
     * It is cumulative: must be called for each reboot.
     */
    public void waitForRebootEndXX()
    {
        try
        {
            this.engineStops.acquire();
        }
        catch (InterruptedException e)
        {
            log.warn("Interruption while waiting for engine to reboot");
        }
        waitForInitEnd();
    }

    public void waitForInitEnd()
    {
        try
        {
            this.engineStarts.acquire();
        }
        catch (InterruptedException e)
        {
            log.warn("Interruption while waiting for engine to start");
        }
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
        log.info(this.localNodeName + " main engine has received a reload request");
        this.run = true;
        this.stop.release();
    }

    public void stopEngine()
    {
        log.info(this.localNodeName + " main engine has received a stop request");
        this.run = false;
        this.stop.release();
    }

    protected void preContextLoad()
    {
        // First start? (only create apps if first start on a console)
        if (!this.runnerMode && !ChronixContext.hasNetworkFile(this.dbPath))
        {
            try
            {
                // Create a default network
                Network n = PlanBuilder.buildLocalDnsNetwork();
                ChronixContext.saveNetwork(n, new File(this.dbPath));

                // Create OPERATIONS application
                Application a = OperationsApplication.getNewApplication(n.getPlace("local"));
                ChronixContext.saveApplicationAndMakeCurrent(a, new File(this.dbPath));

                // Create CHRONIX_MAINTENANCE application
                a = MaintenanceApplication.getNewApplication(n.getPlace("local"));
                ChronixContext.saveApplicationAndMakeCurrent(a, new File(this.dbPath));
            }
            catch (ChronixPlanStorageException e)
            {
                throw new ChronixInitializationException("Could not create default applications", e);
            }
        }
    }

    protected void postContextLoad()
    {
        // Cleanup
        if (!this.runnerMode)
        {
            this.ctx.cleanTransanc();
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
