package org.oxymores.chronix.core.context.api;

import java.util.UUID;

import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.FunctionalOccurrence;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.PlaceGroup;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTOExecutionNode;
import org.oxymores.chronix.dto.DTOFunctionalSequence;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTOSequenceOccurrence;

public class DtoToCore
{
    public static Application getApplication(DTOApplication d, ChronixContextMeta ctx)
    {
        Application a = new Application();
        a.setId(d.getId());
        a.setDescription(d.getDescription());
        a.setName(d.getName());
        a.addVersion(d.getLatestVersionComment());

        for (DTOEventSource dtoSource : d.getEventSources())
        {
            a.addSource(dtoSource, ctx);
        }

        for (DTOPlaceGroup e : d.getGroups())
        {
            a.addGroup(getPlaceGroup(e, a));
        }

        for (DTOFunctionalSequence e : d.getSequences().values())
        {
            a.addCalendar(getCalendar(e, a));
        }

        return a;
    }

    public static FunctionalSequence getCalendar(DTOFunctionalSequence d, Application a)
    {
        FunctionalSequence r = new FunctionalSequence();
        r.setAlertThreshold(d.getAlertThreshold());
        r.setDescription(d.getDescription());
        r.setId(d.getId());
        r.setName(d.getName());
        r.setApplication(a);

        for (DTOSequenceOccurrence dd : d.getDays())
        {
            r.addDay(new FunctionalOccurrence(dd.getLabel()));
        }
        return r;
    }

    /*
     * public static NextOccurrence getNextOccurrence(DTONextOccurrence d, Application a) { NextOccurrence r = new NextOccurrence();
     * r.setDescription(d.getDescription()); r.setId(UUID.fromString(d.getId())); r.setName(d.getName());
     * r.setUpdatedCalendar(a.getCalendar(UUID.fromString(d.getCalendarId())));
     * 
     * return r; }
     */

    public static PlaceGroup getPlaceGroup(DTOPlaceGroup d, Application a)
    {
        PlaceGroup r = new PlaceGroup();
        r.setDescription(d.getDescription());
        r.setId(d.getId());
        r.setName(d.getName());

        // Places <-> Group links are defined inside Places, not Groups.
        return r;
    }

    public static Environment getEnvironment(DTOEnvironment d)
    {
        Environment n = new Environment();

        for (DTOExecutionNode e : d.getNodes())
        {
            n.addNode(getExecutionNode(e));
        }
        for (DTOExecutionNode e : d.getNodes())
        {
            setExecutionNodeNetwork(e, n);
        }

        for (DTOPlace e : d.getPlaces())
        {
            n.addPlace(getPlace(e, n));
        }

        return n;
    }

    /**
     * Pass 1: create nodes without transitions
     * 
     * @param d
     *            the DTO object describing the execution node - a true EN will be created from this.
     * @return the core execution node corresponding to the DTO object.
     */
    public static ExecutionNode getExecutionNode(DTOExecutionNode d)
    {
        ExecutionNode r = new ExecutionNode();
        r.setName(d.getName());
        ExecutionNodeConnectionAmq conn = new ExecutionNodeConnectionAmq();
        conn.setDns(d.getDns());
        conn.setqPort(d.getqPort());
        r.addConnectionMethod(conn);
        r.setId(d.getId());
        r.setJmxRegistryPort(d.getJmxRegistryPort());
        r.setJmxServerPort(d.getJmxServerPort());

        r.setWsPort(d.getWsPort());
        r.setX(d.getX());
        r.setY(d.getY());

        return r;
    }

    /**
     * Pass 2: add connections between nodes. All nodes must exist when this method is called.
     * 
     * @param d
     *            the DTO object describing the node to connect.
     * @param e
     *            the Environment that the new connections should be added to.
     */
    public static void setExecutionNodeNetwork(DTOExecutionNode d, Environment e)
    {
        ExecutionNode from = e.getNode(d.getId());

        if (d.isConsole())
        {
            e.setConsole(from);
        }

        for (String s : d.getToTCP())
        {
            ExecutionNode target = e.getNode(UUID.fromString(s));
            from.connectTo(target, ExecutionNodeConnectionAmq.class);
        }
        for (String s : d.getToRCTRL())
        {
            ExecutionNode target = e.getNode(UUID.fromString(s));
            from.connectTo(target, ExecutionNodeConnectionAmq.class);
            target.setComputingNode(from);
        }
    }

    public static Place getPlace(DTOPlace d, Environment n)
    {
        Place r = new Place();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setNode(n.getNode(d.getNodeid()));
        r.setProperty1(d.getProp1());
        r.setProperty2(d.getProp2());
        r.setProperty3(d.getProp3());
        r.setProperty4(d.getProp4());
        for (UUID s : d.getMemberOf())
        {
            r.addGroupMembership(s);
        }
        return r;
    }

    public static PlaceGroup getPlaceGroup(DTOPlaceGroup d, Environment e)
    {
        PlaceGroup r = new PlaceGroup();
        r.setDescription(d.getDescription());
        r.setId(d.getId());
        r.setName(d.getName());

        // The places inside the group are stored inside the places, not the groups.
        // (they are part of the environment - a deployment property - not of the application definition)
        return r;
    }

    public static FunctionalSequence getFunctionalSequence(DTOFunctionalSequence d)
    {
        FunctionalSequence r = new FunctionalSequence();
        r.setAlertThreshold(d.getAlertThreshold());
        r.setDescription(d.getDescription());
        r.setId(d.getId());
        r.setName(d.getName());

        for (DTOSequenceOccurrence oc : d.getDays())
        {
            FunctionalOccurrence funcOc = new FunctionalOccurrence(oc.getLabel());
            r.addDay(funcOc);
        }
        return r;
    }

    /*
     * public static NextOccurrence getNextOccurrence(DTONextOccurrence d, Application a) { NextOccurrence r = new NextOccurrence();
     * r.setDescription(d.getDescription()); r.setId(UUID.fromString(d.getId())); r.setName(d.getName());
     * r.setUpdatedCalendar(a.getCalendar(UUID.fromString(d.getCalendarId())));
     * 
     * return r; }
     */

    // Places <-> Group links are defined inside Places, not Groups. return r; }
}
