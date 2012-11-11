package org.oxymores.chronix.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.wapi.JettyServer;

public class Scheduler
{
	public static void main(String[] args) throws IOException, InterruptedException
	{
		// Load properties
		Properties applicationProps = new Properties();
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("scheduler.properties");
		// FileInputStream in = new FileInputStream("scheduler.properties");
		applicationProps.load(in);
		in.close();

		String hostname;
		try
		{
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e)
		{
			hostname = "localhost";
		}

		boolean startEngine = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.engine", "true"));
		boolean startWS = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.webserver", "true"));
		// boolean startRunner = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.runner", "true"));
		String repoPath = applicationProps.getProperty("jChronix.repository.path", ".");
		String mainDataInterface = applicationProps.getProperty("jChronix.engine.mainDataInteface", hostname);
		int mainDataPort = Integer.parseInt(applicationProps.getProperty("jChronix.engine.mainDataPort", "1789"));
		String transacUnit = applicationProps.getProperty("jChronix.engine.transacPersistenceUnitName", "TransacUnit");
		String historyUnit = applicationProps.getProperty("jChronix.engine.historyPersistenceUnitName", "HistoryUnit");
		int nbRunner = Integer.parseInt(applicationProps.getProperty("jChronix.runner.maxjobs", "10"));

		// Engine
		ChronixEngine e = null;
		if (startEngine)
		{
			e = new ChronixEngine(repoPath, mainDataInterface + ":" + mainDataPort, transacUnit, historyUnit, false, nbRunner);
			e.start();
			e.waitForInitEnd();
		}

		// Web server
		if (startWS)
		{
			JettyServer server = new JettyServer();
			server.start();
		}

		Thread.sleep(1000000); // 1000s, debug
	}
}
