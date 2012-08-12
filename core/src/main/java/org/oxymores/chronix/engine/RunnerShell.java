package org.oxymores.chronix.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class RunnerShell {
	// RunnerAgent logger, yes.
	private static Logger log = Logger.getLogger(RunnerAgent.class);

	public static RunResult run(RunDescription rd, String logFilePath, boolean storeLogFile, boolean returnFullerLog) {
		RunResult res = new RunResult();
		Process p;
		String nl = System.getProperty("line.separator");
		Pattern pat = Pattern.compile("^set ([a-zA-Z]+[a-zA-Z0-9]*)=(.+)");
		Matcher matcher = pat.matcher("Testing123Testing");

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
			BufferedReader br = new BufferedReader(isr);

			String line = null;
			int i = 0;
			LinkedHashMap<Integer, String> endBuffer = new LinkedHashMap<Integer, String>() {
				private static final long serialVersionUID = -6773540176968046737L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<Integer, String> eldest) {
					return this.size() > 1000;
				}
			};
			Writer output = null;
			if (storeLogFile)
				output = new BufferedWriter(new FileWriter(logFilePath));
			while ((line = br.readLine()) != null) {
				i++;

				// Local log file gets all lines
				if (storeLogFile)
					output.write(line);

				// Small log gets first 500 lines or 10000 characters (the
				// smaller of the two)
				if (i < 500 && res.logStart.length() < 10000)
					res.logStart += nl + line;

				// Scheduler internal log gets first line only
				if (i == 1)
					log.debug(String.format("Job running. First line of output is: %s", line));

				// Fuller log gets first 10k lines, then last 1k lines.
				if (returnFullerLog) {
					if (i < 10000)
						res.fullerLog += line;
					if (i >= 10000)
						endBuffer.put(i, line);
				}

				// Analyse: there may be a new variable definition in the line
				matcher.reset(line);
				if (matcher.find()) {
					log.debug("Key detected :" + matcher.group(1));
					log.debug("Value detected :" + matcher.group(2));
					res.newEnvVars.put(matcher.group(1), matcher.group(2));
				}
			}

			if (i > 10000 && i < 11000 && returnFullerLog)
				res.fullerLog += Arrays.toString(endBuffer.entrySet().toArray());
			if (i >= 11000 && returnFullerLog)
				res.fullerLog += "\n\n\n*******\n LOG TRUNCATED - See full log on server\n********\n\n\n"
						+ Arrays.toString(endBuffer.entrySet().toArray());

			// Done: close log file
			if (storeLogFile)
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
