package org.chronix.cli;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class ChronixCli
{
    private static Logger log = LoggerFactory.getLogger(ChronixCli.class);

    public static void main(String[] args)
    {
        new ChronixCli().doMain(args);
    }

    @Option(name = "-n", aliases = { "--name", "--eventsource" }, usage = "for sending events from this source", required = true)
    private String source;

    @Option(name = "-p", aliases = { "--port" }, usage = "port of the Chronix server to connect to. Default is 1789. ", required = false)
    private Integer port = 1789;

    @Option(name = "-s", aliases = { "--server",
            "--broker" }, usage = "Chronix server to connect to. Default is localhost", required = false)
    private String server = "localhost";

    @Option(name = "-d", aliases = { "--data", "--eventdata" }, usage = "data associated to the event", required = false)
    private String filepath = "";

    @Argument
    private List<String> arguments = new ArrayList<String>();

    private void doMain(String[] args)
    {
        log.info("Starting Chronix CLI");
        CmdLineParser parser = new CmdLineParser(this);

        try
        {
            parser.parseArgument(args);

            if (!arguments.isEmpty())
            {
                throw new CmdLineException(parser, "Weird options given");
            }
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();

            return;
        }

        try
        {
            SenderHelpers.sendOrderExternalEvent(source, filepath, server.toUpperCase());
        }
        catch (JMSException e)
        {
            log.error("An error occurred during message sending: " + e.getMessage());
            System.exit(1);
        }
    }
}
