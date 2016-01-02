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
import org.slf4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.MaintenanceApplication;
import org.oxymores.chronix.planbuilder.OperationsApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * A Chronix Node. Can be either engine + runner or simply runner.
 *
 */
// @Component(enabled = true)
public class ChronixEngine extends Thread
{
    private static Logger log = LoggerFactory.getLogger(ChronixEngine.class);

    private boolean runnerMode;
    private int runnerPort, feederPort;
    private String runnerHost, feederHost;

    protected ChronixContext ctx;
    protected String dbPath;
    protected String historyDbPath, transacDbPath;
    protected String localNodeName;

    protected Broker broker;
    protected SelfTriggerAgent stAgent;

    protected Semaphore stop, stopped, engineStops, engineStarts;
    protected boolean run = true;
    protected int nbRunner;

    // ///////////////////////////////////////////////////////////////
    // Construction
    public ChronixEngine(String dbPath, String nodeName)
    {
        this(dbPath, nodeName, false, 1);
    }

    public ChronixEngine(String dbPath, String nodeName, boolean runnerMode)
    {
        this(dbPath, nodeName, runnerMode, 1);
    }

    public ChronixEngine(String dbPath, String nodeName, boolean runnerMode, int nbRunner)
    {
        this(dbPath, nodeName, runnerMode, nbRunner, FilenameUtils.concat(dbPath, "db_history/db"),
                FilenameUtils.concat(dbPath, "db_transac/db"));
    }

    public ChronixEngine(String dbPath, String nodeName, boolean runnerMode, int nbRunner, String historyDBPath, String transacDbPath)
    {
        this.dbPath = dbPath;
        this.runnerMode = runnerMode;
        this.localNodeName = nodeName;
        this.nbRunner = nbRunner;
        this.historyDbPath = historyDBPath;
        this.transacDbPath = transacDbPath;

        // Putting a token in this will stop the engine
        this.stop = new Semaphore(0);
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

        // Remote nodes must fetch the environment on startup if not already present
        if (!runnerMode && feederHost != null && !ChronixContext.hasEnvironmentFile(this.dbPath))
        {
            BootstrapListener bl = new BootstrapListener(new File(this.dbPath), localNodeName, feederHost, feederPort);
            if (!bl.fetchEnvironment())
            {
                throw new ChronixInitializationException(
                        "Could not fetch environment from remote node. Will not be able to start. See errors above for details.");
            }
        }

        // Context
        if (!runnerMode)
        {
            preContextLoad();
            this.ctx = new ChronixContext(this.localNodeName, this.dbPath, false, this.historyDbPath, this.transacDbPath);
            this.ctx.getHistoryDataSource(); // Force DB init to get errors at once
            this.ctx.getTransacDataSource();
            postContextLoad();
        }

        // Mock-up for hosted nodes
        if (runnerMode)
        {
            ExecutionNode e = new ExecutionNode();
            ExecutionNodeConnectionAmq c = new ExecutionNodeConnectionAmq();

            e.setName(this.localNodeName);
            c.setDns(this.runnerHost);
            c.setqPort(this.runnerPort);
            e.addConnectionMethod(c);
            this.ctx = new ChronixContext("runner", this.dbPath, false, null, null);
            this.ctx.setLocalNode(e);
        }

        // Broker with all the consumer threads
        if (this.broker == null)
        {
            this.broker = new Broker(this.ctx, false, !this.runnerMode, true);
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
                log.error("The engine has failed to start", e);
                this.run = false;
                this.stop.release();
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

    /** Test method only */
    void stopOutgoingJms()
    {
        this.broker.stopAllOutgoingLinks();
    }

    protected void preContextLoad()
    {
        // First start? (only create apps if first start on a console)
        if (!this.runnerMode && !ChronixContext.hasEnvironmentFile(this.dbPath))
        {
            try
            {
                // Create a default environment
                Environment n = PlanBuilder.buildLocalDnsNetwork();
                ChronixContext.saveEnvironment(n, new File(this.dbPath));

                // Create OPERATIONS application
                Application a = OperationsApplication.getNewApplication(n.getPlace("local"));
                ChronixContext.saveApplication(a, new File(this.dbPath));

                // Create CHRONIX_MAINTENANCE application
                a = MaintenanceApplication.getNewApplication(n.getPlace("local"));
                ChronixContext.saveApplication(a, new File(this.dbPath));
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

    public void setLocalNodeName(String name)
    {
        this.localNodeName = name;
    }

    public void setFeeder(ExecutionNode en)
    {
        ExecutionNodeConnectionAmq conn = en.getComputingNode().getConnectionParameters(ExecutionNodeConnectionAmq.class).get(0);
        this.setFeeder(conn.getDns(), conn.getqPort());
    }
}
