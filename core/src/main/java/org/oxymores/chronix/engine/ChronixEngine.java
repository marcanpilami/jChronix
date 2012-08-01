package org.oxymores.chronix.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.demo.MaintenanceApplication;
import org.oxymores.chronix.demo.OperationsApplication;
import org.oxymores.chronix.exceptions.IncorrectConfigurationException;

public class ChronixEngine {
	private static Logger log = Logger.getLogger(ChronixEngine.class);

	public Broker broker;
	public ChronixContext ctx;
	public String dbPath;

	public ChronixEngine(String dbPath) {
		this.dbPath = dbPath;
	}

	public void start() throws Exception {
		preContextLoad();
		ctx = ChronixContext.loadContext(dbPath);
		postContextLoad();

		broker = new Broker(ctx, false);
	}

	public void stop() {
		broker.stop();
	}

	public void restart() {

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
			log.error("The listener.crn file was not found in folder " + dbPath
					+ " - scheduler cannot start");
			throw new IncorrectConfigurationException(
					"listener.crn file missing from configuration database");
		}
	}

	protected void postContextLoad() throws Exception {
		// First start?
		if (this.ctx.applicationsById.values().size() == 0) {
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

	public void injectListenerConfigIntoDb() throws IOException {
		try {
			injectListenerConfigIntoDb(InetAddress.getLocalHost()
					.getCanonicalHostName() + ":1789");
		} catch (UnknownHostException e) {
			injectListenerConfigIntoDb("localhost" + ":1400");
		}
	}

	public void injectListenerConfigIntoDb(String url) throws IOException {
		File f = new File(dbPath + "/listener.crn");
		Writer output = new BufferedWriter(new FileWriter(f));
		output.write(url);
		output.close();
	}
}
