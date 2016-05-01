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
package org.oxymores.chronix.agent.command.shellrunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.agent.command.api.CommandDescription;
import org.oxymores.chronix.agent.command.api.CommandResult;
import org.oxymores.chronix.agent.command.api.CommandRunner;
import org.oxymores.chronix.agent.command.api.RunnerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = { "target=" + RunnerConstants.SHELL_POWERSHELL, "target=" + RunnerConstants.SHELL_WINCMD,
        "target=" + RunnerConstants.SHELL_BASH, "target=" + RunnerConstants.SHELL_KSH,
        "target=" + RunnerConstants.SHELL_SH }, immediate = false)
public final class RunnerShell implements CommandRunner
{
    private static final Logger log = LoggerFactory.getLogger(RunnerShell.class);

    @Override
    public CommandResult run(CommandDescription rd)
    {
        CommandResult res = new CommandResult();
        Process p;
        String nl = System.getProperty("line.separator");
        Pattern setVarPattern = Pattern.compile("^set ([a-zA-Z]+[a-zA-Z0-9]*)=(.+)");
        Matcher setVarMatcher = setVarPattern.matcher("Testing123Testing");
        String logFileEncoding = getEncoding();
        log.debug("Encoding is " + logFileEncoding);

        // ///////////////////////////
        // Build command
        List<String> argsStrings = buildCommand(rd);
        log.debug("Runner shell will run " + argsStrings);

        // /////////////////////////////////////////////////////////////////////////
        // Create a process builder with the command line contained in the array
        ProcessBuilder pb = new ProcessBuilder(argsStrings);

        // Mix stdout and stderr (easier to put errors in context this way)
        pb.redirectErrorStream(true);

        // Create array containing environment
        Map<String, String> env = pb.environment();
        for (Map.Entry<String, String> var : rd.getEnvironmentVariables().entrySet())
        {
            env.put(var.getKey(), var.getValue());
        }

        BufferedReader br = null;
        Writer output = null, subWriter = null;
        try
        {
            // Start!
            p = pb.start();

            // Read output (err & out), write it to file
            InputStreamReader isr = new InputStreamReader(p.getInputStream(), logFileEncoding);
            br = new BufferedReader(isr);

            String line;
            int i = 0;
            LinkedHashMap<Integer, String> endBuffer = new LinkedHashMap<Integer, String>()
            {
                private static final long serialVersionUID = -6773540176968046737L;

                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Integer, String> eldest)
                {
                    return this.size() > RunnerConstants.MAX_RETURNED_BIG_LOG_END_LINES;
                }
            };

            if (rd.isStoreLogFile())
            {
                subWriter = new FileWriterWithEncoding(rd.getLogFilePath(), "UTF-8");
                output = new BufferedWriter(subWriter);
            }
            line = br.readLine();
            while (line != null)
            {
                i++;

                // Local log file gets all lines
                if (rd.isStoreLogFile())
                {
                    output.write(line + nl);
                }

                // Small log gets first 500 lines or 10000 characters (the smaller of the two)
                if (i < RunnerConstants.MAX_RETURNED_SMALL_LOG_LINES
                        && res.logStart.length() < RunnerConstants.MAX_RETURNED_SMALL_LOG_CHARACTERS)
                {
                    res.logStart += nl + line;
                }

                // Scheduler internal log (stored in database) gets first line only
                if (i == 1)
                {
                    log.debug(String.format("Job %s running. First line of output is: %s", rd.getLaunchId(), line));
                }

                // Fuller log gets first 10k lines, then last 1k lines.
                if (i < RunnerConstants.MAX_RETURNED_BIG_LOG_LINES)
                {
                    res.fullerLog += line;
                }
                else
                {
                    endBuffer.put(i, line);
                }

                // Analysis: there may be a new variable definition in the line
                setVarMatcher.reset(line);
                if (setVarMatcher.find())
                {
                    log.debug("Key detected :" + setVarMatcher.group(1));
                    log.debug("Value detected :" + setVarMatcher.group(2));
                    res.newEnvVars.put(setVarMatcher.group(1), setVarMatcher.group(2));
                }
                line = br.readLine();
            }
            IOUtils.closeQuietly(br);

            if (i > RunnerConstants.MAX_RETURNED_BIG_LOG_LINES
                    && i < RunnerConstants.MAX_RETURNED_BIG_LOG_LINES + RunnerConstants.MAX_RETURNED_BIG_LOG_END_LINES)
            {
                res.fullerLog += Arrays.toString(endBuffer.entrySet().toArray());
            }
            if (i >= RunnerConstants.MAX_RETURNED_BIG_LOG_LINES + RunnerConstants.MAX_RETURNED_BIG_LOG_END_LINES)
            {
                res.fullerLog += "\n\n\n*******\n LOG TRUNCATED - See full log on server\n********\n\n\n"
                        + Arrays.toString(endBuffer.entrySet().toArray());
            }

            // Done: close log file
            if (rd.isStoreLogFile())
            {
                IOUtils.closeQuietly(output);
                File f = new File(rd.getLogFilePath());
                res.logSizeBytes = f.length();
            }
        }
        catch (IOException e)
        {
            log.error("error occurred while running job", e);
            res.logStart = e.getMessage();
            res.returnCode = -1;
            return res;
        }
        finally
        {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(subWriter);
        }

        // Return
        res.returnCode = p.exitValue();
        res.logPath = rd.getLogFilePath();
        res.envtUser = System.getProperty("user.name");
        try
        {
            res.envtServer = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            log.debug("cannot find host - 'unknown' will be used instead", e);
            res.envtServer = "unknown";
        }
        log.info(String.format("Job %s ended, RC is %s", rd.getLaunchId(), res.returnCode));
        return res;
    }

    private static String getEncoding()
    {
        String encoding = System.getProperty("file.encoding");
        if (System.getProperty("os.name").startsWith("Windows"))
        {
            try
            {
                encoding = "cp" + WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
                        "SYSTEM\\CurrentControlSet\\Control\\Nls\\CodePage", "OEMCP");
            }
            catch (Exception e)
            {
                log.warn("Windows console encoding could not be found. Default encoding will be used. Logs may be encoded weirdly", e);
            }
        }
        return encoding;
    }

    private static List<String> buildCommand(CommandDescription rd)
    {
        ArrayList<String> argsStrings = new ArrayList<>();

        // Depending on the shell, we may have to add shell start parameters to allow batch processing
        switch (rd.getRunnerCapability())
        {
        case RunnerConstants.SHELL_WINCMD:
            argsStrings.add("cmd.exe");
            argsStrings.add("/C");
            break;
        case RunnerConstants.SHELL_POWERSHELL:
            argsStrings.add("powershell.exe");
            argsStrings.add("-NoLogo");
            argsStrings.add("-NonInteractive");
            argsStrings.add("-WindowStyle");
            argsStrings.add("Hidden");
            argsStrings.add("-Command");
            break;
        case RunnerConstants.SHELL_BASH:
            argsStrings.add("/bin/bash");
            argsStrings.add("-c");
            break;
        case RunnerConstants.SHELL_SH:
            argsStrings.add("/bin/sh");
            argsStrings.add("-c");
            break;
        case RunnerConstants.SHELL_KSH:
            argsStrings.add("/bin/ksh");
            argsStrings.add("-c");
            break;
        default:
            throw new IllegalArgumentException("unknown shell");
        }

        // Then add the command itself
        argsStrings.add(rd.getPluginParameters().get("COMMAND"));

        // Finally add parameters (if any - there may be none or they may be contained inside the command itself)
        for (Map.Entry<String, String> prm : rd.getParameters())
        {
            String key = prm.getKey();
            String value = prm.getValue();
            String arg = "";
            if (key != null && !"".equals(key))
            {
                arg = key;
            }
            if (value != null && !"".equals(value) && (key != null && !"".equals(key)))
            {
                arg += " " + value;
            }
            if (value != null && !"".equals(value) && !(key != null && !"".equals(key)))
            {
                arg += value;
            }

            argsStrings.add(arg);
        }

        return argsStrings;
    }
}
