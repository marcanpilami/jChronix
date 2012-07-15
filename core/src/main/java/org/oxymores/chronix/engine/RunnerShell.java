package org.oxymores.chronix.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

public class RunnerShell {
	// RunnerAgent logger, yes.
	private static Logger log = Logger.getLogger(RunnerAgent.class);

	public static RunResult run(RunDescription rd) {
		RunResult res = new RunResult();
		String logFilePath = "C:\\TEMP\\db1\\test.txt";
		Process p;
		
		// Create array containing arguments
		ArrayList<String> argsStrings = new ArrayList<String>();
		argsStrings.add("cmd.exe");
		argsStrings.add("/C");
		argsStrings.add(rd.command);
		for (int i = 0; i < rd.paramNames.size(); i++) {
			String key = rd.paramNames.get(i);
			String value = rd.paramValues.get(i);
			String arg = "";
			if (key != null && !key.equals(""))
				arg = key;
			if (value != null && !value.equals("") && (key != null && !key.equals("")))
				arg += " " + value;
			if (value != null && !value.equals("") && !(key != null && !key.equals("")))
				arg += value;

			argsStrings.add(arg);
		}

		// Create a process builder with the command line contained in the array
		ProcessBuilder pb = new ProcessBuilder(argsStrings);

		// Mix stdout and stderr (easier to put errors in context this way)
		pb.redirectErrorStream(true);

		// Create array containing environment
		Map<String, String> env = pb.environment();
		for (int i = 0; i < rd.envNames.size(); i++) {
			env.put(rd.envNames.get(i), rd.envValues.get(i));
		}
		// pb.directory("myDir");
		
		
		try {
			// Start!
			log.debug("GO");
			p = pb.start();

			// Read output (err & out), write it to file
			InputStreamReader isr = new InputStreamReader(p.getInputStream());
			Writer output = new BufferedWriter(new FileWriter(logFilePath));
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			int i = 0;
			while ((line = br.readLine()) != null) {
				output.write(line);
				i++;
				if (i < 500)
					res.logStart += line;
				if (i == 1)
					log.debug(String.format("Job running. First line of output is: %s", line));
			}
			
			// Done: close log file
			output.close();
		} catch (IOException e) {
			log.error("error occurred while running job", e);
			res.logStart = e.getMessage();
			res.returnCode = -1;
			return res;
		}

		// Return 
		res.returnCode = p.exitValue();
		res.logPath = logFilePath;
		log.info(String.format("Job ended, RC is %s", res.returnCode));
		return res;
	}
}
