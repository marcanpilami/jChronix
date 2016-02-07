package org.oxymores.chronix.engine;


/*
public class ChronixEngineSim extends ChronixEngine
{
    private static final Logger log = LoggerFactory.getLogger(ChronixEngineSim.class);

    private final UUID appToSimulateId;
    private final DateTime start, end;

    public static List<RunLog> simulate(String configurationDirectoryPath, UUID appID, DateTime start, DateTime end)
    {
        ChronixEngineSim es = new ChronixEngineSim(configurationDirectoryPath, appID, start, end);
        es.startEngine(false);
        return es.waitForSimEnd();
    }

    public ChronixEngineSim(String configurationDirectoryPath, UUID appID, DateTime start, DateTime end)
    {
        super(configurationDirectoryPath, "raccoon:9999", false, 0);
        this.appToSimulateId = appID;
        this.start = start;
        this.end = end;
    }

    // params are ignored!
    @Override
    protected void startEngine(boolean blocking)
    {
        MDC.put("node", "simu");
        log.info(String.format("(%s) simulation engine starting between %s and %s", this.dbPath, this.start, this.end));
        try
        {
            // Context
            this.ctxMeta = new ChronixContext("simu", this.dbPath, true, null, null);

            // This is a simulation: we are only interested in a single application
            for (Application ap : this.ctxMeta.getApplications())
            {
                if (!ap.getId().equals(appToSimulateId))
                {
                    this.ctxMeta.removeApplicationFromCache(ap.getId());
                }
            }

            // This is a simulation: there is no network, only one simulation node.
            ExecutionNode simulationNode = new ExecutionNode();
            ExecutionNodeConnectionAmq conn = new ExecutionNodeConnectionAmq();
            conn.setDns("raccoon");
            conn.setqPort(9999);
            simulationNode.addConnectionMethod(conn);
            simulationNode.setName("simu");
            this.ctxMeta.getEnvironment().addNode(simulationNode);
            this.ctxMeta.getEnvironment().setConsole(simulationNode);
            ctxMeta.setLocalNodeName(simulationNode.getName());

            for (Place p : this.ctxMeta.getEnvironment().getPlaces().values())
            {
                p.setNode(simulationNode);
            }

            // Broker with some of the consumer threads. Not started: meta, runner agent, order. In memory broker, no networking, with EM.
            this.broker = new Broker(this.ctxMeta, false, true, false);
            this.broker.setNbRunners(this.nbRunner);
            this.broker.registerListeners(this, false, false, true, true, true, true, true, false, true);

            // Active sources agent
            this.stAgent = new SelfTriggerAgentSim();
            ((SelfTriggerAgentSim) this.stAgent).setBeginTime(start);
            ((SelfTriggerAgentSim) this.stAgent).setEndTime(end);
            this.stAgent.startAgent(ctxMeta, broker.getConnection(), this.start);

            // Done
            this.engineStarts.release();
            log.info("Simulator for context " + this.ctxMeta.getContextRoot() + " has finished its boot sequence");

        }
        catch (ChronixInitializationException | JMSException | IOException e)
        {
            log.error("The simulation engine has failed to start", e);
            this.run = false;
        }
    }

    public List<RunLog> waitForSimEnd()
    {
        this.waitForInitEnd();
        log.info("Simulation has started. Waiting for simulation end");
        try
        {
            this.stAgent.join();
        }
        catch (InterruptedException e)
        {
        }
        log.info("Simulation has ended. Returning results");
        MDC.remove("node");

        try (Connection conn = this.ctxMeta.getHistoryDataSource().open())
        {
            return conn.createQuery("SELECT * from RunLog h").executeAndFetch(RunLog.class);
        }
    }
}
*/