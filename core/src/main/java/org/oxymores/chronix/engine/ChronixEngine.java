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
import org.oxymores.chronix.demo.MaintenanceApplication;
import org.oxymores.chronix.demo.OperationsApplication;

/**
 * A Chronix Node. Can be either engine + runner or simply runner.
 *
 */
public class ChronixEngine extends Thread
{
	private static Logger log = Logger.getLogger(ChronixEngine.class);

	private boolean runnerMode;

	public ChronixContext ctx;
	public String dbPath;
	public String brokerInterface, transacUnitName, historyUnitName;
	public int brokerPort;
	public String brokerInterfaceNoPort;

	public Broker broker;
	public SelfTriggerAgent stAgent;

	private Semaphore startCritical, stop, threadInit, stopped;
	boolean run = true;
	int nbRunner;

	// ///////////////////////////////////////////////////////////////
	// Construction
	public ChronixEngine(String dbPath, String mainInterface)
	{
		this(dbPath, mainInterface, "TransacUnit", "HistoryUnit");
	}

	public ChronixEngine(String dbPath, String mainInterface, String TransacUnitName, String HistoryUnitName)
	{
		this(dbPath, mainInterface, TransacUnitName, HistoryUnitName, false, 1);
	}

	public ChronixEngine(String dbPath, String mainInterface, String TransacUnitName, String HistoryUnitName, boolean runnerMode)
	{
		this(dbPath, mainInterface, TransacUnitName, HistoryUnitName, runnerMode, 1);
	}

	public ChronixEngine(String dbPath, String mainInterface, String TransacUnitName, String HistoryUnitName, boolean runnerMode,
			int nbRunner)
	{
		this.dbPath = dbPath;
		this.runnerMode = runnerMode;
		this.transacUnitName = TransacUnitName;
		this.historyUnitName = HistoryUnitName;
		this.brokerInterface = mainInterface;
		this.ctx = new ChronixContext(); // to allow some basic config
		this.ctx.configurationDirectoryPath = dbPath; // before starting
		this.ctx.configurationDirectory = new File(dbPath);
		this.ctx.localUrl = mainInterface;
		this.ctx.transacUnitName = TransacUnitName;
		this.ctx.historyUnitName = HistoryUnitName;
		this.brokerInterfaceNoPort = this.brokerInterface.split(":")[0];
		this.brokerPort = Integer.parseInt(this.brokerInterface.split(":")[1]);
		this.nbRunner = nbRunner;

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
				this.broker.registerListeners(this);
			else
				this.broker.registerListeners(this, false, true, false, false, false, false, false, false, false);

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
				stop.wait();
			} /*
			 * else this.start();
			 */

		} catch (Exception e)
		{
			log.error("The engine has failed to start", e);
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
			} catch (InterruptedException e)
			{
				log.error("big problem here", e);
			}

			// Properly stop the engine
			if (this.stAgent != null)
				this.stAgent.stopAgent();
			this.broker.stop();

			try
			{
				Thread.sleep(1000);
			} catch (InterruptedException e)
			{
			} // Port release is not immediate, sad.

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
		} catch (InterruptedException e)
		{
		}
		this.startCritical.release();
		this.threadInit.release();
	}

	public void waitForStopEnd()
	{
		try
		{
			this.stopped.acquire();
		} catch (InterruptedException e)
		{
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
		} catch (InterruptedException e)
		{
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
		} catch (InterruptedException e)
		{
		}
		this.startCritical.release();
	}

	protected void preContextLoad() throws Exception
	{

	}

	protected void postContextLoad() throws Exception
	{
		// First start?
		if (this.ctx.applicationsById.values().size() == 0 && !this.runnerMode)
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
	}

	public void emptyDb()
	{
		// Clear test db directory
		File[] fileList = new File(this.dbPath).listFiles();
		for (int i = 0; i < fileList.length; i++)
			fileList[i].delete();
	}
}
