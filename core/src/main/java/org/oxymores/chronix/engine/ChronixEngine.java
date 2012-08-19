package org.oxymores.chronix.engine;

import java.io.File;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.demo.MaintenanceApplication;
import org.oxymores.chronix.demo.OperationsApplication;
import org.oxymores.chronix.exceptions.IncorrectConfigurationException;

public class ChronixEngine extends Thread {
	private static Logger log = Logger.getLogger(ChronixEngine.class);

	private boolean runnerMode;

	public ChronixContext ctx;
	public String dbPath;

	public Broker broker;
	public SelfTriggerAgent stAgent;

	private Semaphore startCritical, stop, threadInit, stopped;
	boolean run = true;

	public ChronixEngine(String dbPath) {
		this(dbPath, false);
	}

	public ChronixEngine(String dbPath, boolean runnerMode) {
		this.dbPath = dbPath;
		this.runnerMode = runnerMode;
		this.ctx = new ChronixContext(); // to allow some basic config
		this.ctx.configurationDirectoryPath = dbPath; // before starting
		this.ctx.configurationDirectory = new File(dbPath);

		this.startCritical = new Semaphore(1);
		this.stop = new Semaphore(0);
		this.threadInit = new Semaphore(0);
		this.stopped = new Semaphore(0);
	}

	private void startEngine(boolean blocking, boolean purgeQueues) {
		log.info(String.format("(%s) engine starting (%s)", this.dbPath, this));
		try {
			this.startCritical.acquire();
			this.threadInit.release(1);

			// Context
			preContextLoad();
			this.ctx = ChronixContext.loadContext(this.dbPath);
			postContextLoad();

			// Broker with all the consumer threads
			this.broker = new Broker(this, purgeQueues);
			if (!runnerMode)
				this.broker.registerListeners(this);
			else
				this.broker.registerListeners(this, false, true, false, false, false, false, false, false, false);

			// Active sources agent
			if (!this.runnerMode && broker.getEmf() != null) {
				this.stAgent = new SelfTriggerAgent();
				this.stAgent.startAgent(broker.getEmf(), ctx, broker.getConnection());
			}

			// Done
			this.startCritical.release();

			if (blocking) {
				stop.wait();
			} /*
			 * else this.start();
			 */

		} catch (Exception e) {
			log.error("The engine has failed to start", e);
			this.run = false;
		}
	}

	@Override
	public void run() {
		// Only does one thing: reload configuration and stop on trigger
		while (this.run) {
			// First : start!
			startEngine(false, false);
			if (!run) {
				this.startCritical.release();
				this.stop.release();
				this.threadInit.release();
				return;
			}

			// Then, wait for the end signal
			try {
				this.stop.acquire();
			} catch (InterruptedException e) {
				log.error("big problem here", e);
			}

			// Properly stop the engine
			if (this.stAgent != null)
				this.stAgent.stopAgent();
			this.broker.stop();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			} // Port release is not immediate, sad.

			// Done. If 'run' is still true, will restart the engine
		}
		this.stopped.release();
		log.info("The scheduler has stopped");
	}

	public void waitForInitEnd() {
		try {
			this.threadInit.acquire();
			this.startCritical.acquire();
		} catch (InterruptedException e) {
		}
		this.startCritical.release();
		this.threadInit.release();
	}

	public void waitForStopEnd() {
		try {
			this.stopped.acquire();
		} catch (InterruptedException e) {
		}
		this.stop.release();
	}

	public void queueReloadConfiguration() {
		try {
			this.startCritical.acquire();
			threadInit.acquire();
			this.run = true;
			this.stop.release();
		} catch (InterruptedException e) {
		}
		this.startCritical.release();
	}

	public void stopEngine() {
		log.info("The main engine has received a stop request");
		try {
			this.startCritical.acquire();
			this.run = false;
			this.stop.release();
		} catch (InterruptedException e) {
		}
		this.startCritical.release();
	}

	protected void preContextLoad() throws Exception {
		File[] fileList = new File(this.dbPath).listFiles();
		Boolean found = false;
		if (fileList != null) {
			for (File f : fileList) {
				if (f.isFile() && f.getName().equals("listener.crn")) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			log.error("The listener.crn file was not found in folder " + dbPath + " - scheduler cannot start");
			this.run = false;
			throw new IncorrectConfigurationException("listener.crn file missing from configuration database");
		}
	}

	protected void postContextLoad() throws Exception {
		// First start?
		if (this.ctx.applicationsById.values().size() == 0 && !this.runnerMode) {
			// Create OPERATIONS application
			Application a = OperationsApplication.getNewApplication();
			this.ctx.saveApplication(a);
			this.ctx.setWorkingAsCurrent(a);

			// Create CHRONIX_MAINTENANCE application
			a = MaintenanceApplication.getNewApplication();
			this.ctx.saveApplication(a);
			this.ctx.setWorkingAsCurrent(a);

			// Reload context to load new applications
			this.ctx = ChronixContext.loadContext(this.dbPath);
		}
	}

	public void emptyDb() {
		// Clear test db directory
		File[] fileList = new File(this.dbPath).listFiles();
		for (int i = 0; i < fileList.length; i++)
			fileList[i].delete();
	}
}
