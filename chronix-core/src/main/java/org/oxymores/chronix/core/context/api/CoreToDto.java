package org.oxymores.chronix.core.context.api;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.EventSourceDef;
import org.oxymores.chronix.core.app.FunctionalOccurrence;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.ParameterDef;
import org.oxymores.chronix.core.app.PlaceGroup;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.engine.api.DTOToken;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.DTOClock;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTOExecutionNode;
import org.oxymores.chronix.dto.DTOFunctionalSequence;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.DTOSequenceOccurrence;

public class CoreToDto
{
    public static DTOApplication getApplication(Application a)
    {
        DTOApplication res = new DTOApplication();

        res.setId(a.getId());
        res.setName(a.getName());
        res.setDescription(a.getDescription());

        res.setVersion(a.getVersion());
        res.setLatestVersionComment(a.getCommitComment());

        // Event sources of all kinds
        for (EventSourceDef nb : a.getEventSources().values())
        {
            res.addEventSource(nb.getDTO());
        }

        Comparator<PlaceGroup> comparator_pg = new Comparator<PlaceGroup>()
        {
            @Override
            public int compare(PlaceGroup c1, PlaceGroup c2)
            {
                return c1.getName().compareToIgnoreCase(c2.getName());
            }
        };
        List<PlaceGroup> pgs = a.getGroupsList();
        Collections.sort(pgs, comparator_pg);
        for (PlaceGroup pg : pgs)
        {
            res.getGroups().add(getPlaceGroup(pg));
        }

        // Calendars
        for (FunctionalSequence c : a.getCalendars())
        {
            res.addSequence(getFunctionalSequence(c));
        }

        // Tokens
        for (Map.Entry<UUID, DTOToken> e : a.getTokens().entrySet())
        {
            res.addToken(e.getValue());
        }

        // Parameters
        for (Map.Entry<UUID, ParameterDef> e : a.getSharedParameters().entrySet())
        {
            res.addSharedParameter(e.getKey(), e.getValue().getDTO());
        }

        return res;
    }

    public static DTOExecutionNode getExecutionNode(ExecutionNode en)
    {
        DTOExecutionNode res = new DTOExecutionNode();
        res.setId(en.getId());
        res.setConsole(en.getEnvironment().getConsole() == en);
        res.setJmxServerPort(en.getJmxServerPort());
        res.setJmxRegistryPort(en.getJmxRegistryPort());
        res.setWsPort(en.getWsPort());
        res.setX(en.getX());
        res.setY(en.getY());
        res.setName(en.getName());

        // Simplified API - only one AMQ connection allowed.
        for (ExecutionNodeConnectionAmq conn : en.getConnectionParameters(ExecutionNodeConnectionAmq.class))
        {
            res.setDns(conn.getDns());
            res.setqPort(conn.getqPort());
        }

        for (ExecutionNode on : en.getEnvironment().getNodesList())
        {
            if (on.getComputingNode() == en)
            {
                res.addToRctrl(on.getId());
            }
        }
        for (ExecutionNodeConnectionAmq conn : en.getConnectsTo(ExecutionNodeConnectionAmq.class))
        {
            for (ExecutionNode on : en.getEnvironment().getNodesList())
            {
                for (ExecutionNodeConnectionAmq ccc : on.getConnectionParameters(ExecutionNodeConnectionAmq.class))
                {
                    if (ccc == conn && !res.getToRCTRL().contains(on.getId().toString()))
                    {
                        res.addToTcp(on.getId());
                    }
                }
            }
        }

        for (Place p : en.getPlacesHosted())
        {
            res.addPlace(p.getId());
        }

        res.setSimpleRunner(en.isHosted());

        return res;
    }

    public static DTOPlace getPlace(Place p)
    {
        DTOPlace res = new DTOPlace(p.getName(), p.getNode().getId(), p.getProperty1(), p.getProperty2(), p.getProperty3(),
                p.getProperty4(), p.getId());

        for (UUID pg : p.getMemberOfIds())
        {
            res.addMemberOfGroup(pg);
        }
        return res;
    }

    public static DTOPlaceGroup getPlaceGroup(PlaceGroup g)
    {
        DTOPlaceGroup res = new DTOPlaceGroup(g.getName(), g.getDescription(), g.getId());
        return res;
    }

    public static DTOClock getClock(Clock c)
    {
        DTOClock res = new DTOClock();
        /*
         * res.setDescription(c.getDescription()); res.setId(c.getId().toString()); res.setName(c.getName());
         * 
         * for (ClockRRule r : c.getRulesADD()) { res.addRuleAdd(r.getId()); } for (ClockRRule r : c.getRulesEXC()) {
         * res.addRuleExc(r.getId()); }
         */
        return res;
    }

    /*
     * public static DTORRule getRRule(ClockRRule r) { DTORRule res = new DTORRule();
     * 
     * // Identification res.setId(r.getId().toString()); res.setName(r.getName()); res.setDescription(r.getDescription());
     * 
     * // Period res.setPeriod(r.getPeriod()); res.setInterval(r.getINTERVAL());
     * 
     * // ByDay for (String d : r.getBYDAY().split(",")) { if (d.equals("MO")) { res.setBd01(true); } else if (d.equals("TU")) {
     * res.setBd02(true); } else if (d.equals("WE")) { res.setBd03(true); } else if (d.equals("TH")) { res.setBd04(true); } else if
     * (d.equals("FR")) { res.setBd05(true); } else if (d.equals("SA")) { res.setBd06(true); } else if (d.equals("SU")) { res.setBd07(true);
     * } } // ByMonthDay for (String d : r.getBYMONTHDAY().split(",")) { if (d.equals("01")) { res.setBmd01(true); } else if
     * (d.equals("-01")) { res.setBmdn01(true); } else if (d.equals("02")) { res.setBmd02(true); } else if (d.equals("-02")) {
     * res.setBmdn02(true); } else if (d.equals("03")) { res.setBmd03(true); } else if (d.equals("-03")) { res.setBmdn03(true); } else if
     * (d.equals("04")) { res.setBmd04(true); } else if (d.equals("-04")) { res.setBmdn04(true); } else if (d.equals("05")) {
     * res.setBmd05(true); } else if (d.equals("-05")) { res.setBmdn05(true); } else if (d.equals("06")) { res.setBmd06(true); } else if
     * (d.equals("-06")) { res.setBmdn06(true); } else if (d.equals("07")) { res.setBmd07(true); } else if (d.equals("-07")) {
     * res.setBmdn07(true); } else if (d.equals("08")) { res.setBmd08(true); } else if (d.equals("-08")) { res.setBmdn08(true); } else if
     * (d.equals("09")) { res.setBmd09(true); } else if (d.equals("-09")) { res.setBmdn09(true); } else if (d.equals("10")) {
     * res.setBmd10(true); } else if (d.equals("-10")) { res.setBmdn10(true); } else if (d.equals("11")) { res.setBmd11(true); } else if
     * (d.equals("-11")) { res.setBmdn11(true); } else if (d.equals("12")) { res.setBmd12(true); } else if (d.equals("-12")) {
     * res.setBmdn12(true); } else if (d.equals("13")) { res.setBmd13(true); } else if (d.equals("-13")) { res.setBmdn13(true); } else if
     * (d.equals("14")) { res.setBmd14(true); } else if (d.equals("-14")) { res.setBmdn14(true); } else if (d.equals("15")) {
     * res.setBmd15(true); } else if (d.equals("-15")) { res.setBmdn15(true); } else if (d.equals("16")) { res.setBmd16(true); } else if
     * (d.equals("-16")) { res.setBmdn16(true); } else if (d.equals("17")) { res.setBmd17(true); } else if (d.equals("-17")) {
     * res.setBmdn17(true); } else if (d.equals("18")) { res.setBmd18(true); } else if (d.equals("-18")) { res.setBmdn18(true); } else if
     * (d.equals("19")) { res.setBmd19(true); } else if (d.equals("-19")) { res.setBmdn19(true); } else if (d.equals("20")) {
     * res.setBmd20(true); } else if (d.equals("-20")) { res.setBmdn20(true); } else if (d.equals("21")) { res.setBmd21(true); } else if
     * (d.equals("-21")) { res.setBmdn21(true); } else if (d.equals("22")) { res.setBmd22(true); } else if (d.equals("-22")) {
     * res.setBmdn22(true); } else if (d.equals("23")) { res.setBmd23(true); } else if (d.equals("-23")) { res.setBmdn23(true); } else if
     * (d.equals("24")) { res.setBmd24(true); } else if (d.equals("-24")) { res.setBmdn24(true); } else if (d.equals("25")) {
     * res.setBmd25(true); } else if (d.equals("-25")) { res.setBmdn25(true); } else if (d.equals("26")) { res.setBmd26(true); } else if
     * (d.equals("-26")) { res.setBmdn26(true); } else if (d.equals("27")) { res.setBmd27(true); } else if (d.equals("-27")) {
     * res.setBmdn27(true); } else if (d.equals("28")) { res.setBmd28(true); } else if (d.equals("-28")) { res.setBmdn29(true); } else if
     * (d.equals("29")) { res.setBmd29(true); } else if (d.equals("-29")) { res.setBmdn29(true); } else if (d.equals("30")) {
     * res.setBmd30(true); } else if (d.equals("-30")) { res.setBmdn30(true); } else if (d.equals("31")) { res.setBmd31(true); } else if
     * (d.equals("-31")) { res.setBmdn31(true); } } // ByMonth for (String d : r.getBYMONTH().split(",")) { if (d.equals("01")) {
     * res.setBm01(true); } else if (d.equals("02")) { res.setBm02(true); } else if (d.equals("03")) { res.setBm03(true); } else if
     * (d.equals("04")) { res.setBm04(true); } else if (d.equals("05")) { res.setBm05(true); } else if (d.equals("06")) { res.setBm06(true);
     * } else if (d.equals("07")) { res.setBm07(true); } else if (d.equals("08")) { res.setBm08(true); } else if (d.equals("09")) {
     * res.setBm09(true); } else if (d.equals("10")) { res.setBm10(true); } else if (d.equals("11")) { res.setBm11(true); } else if
     * (d.equals("12")) { res.setBm12(true); } } // ByHour for (String d : r.getBYHOUR().split(",")) { if (d.equals("00")) {
     * res.setBh00(true); } else if (d.equals("01")) { res.setBh01(true); } else if (d.equals("02")) { res.setBh02(true); } else if
     * (d.equals("03")) { res.setBh03(true); } else if (d.equals("04")) { res.setBh04(true); } else if (d.equals("05")) { res.setBh05(true);
     * } else if (d.equals("06")) { res.setBh06(true); } else if (d.equals("07")) { res.setBh07(true); } else if (d.equals("08")) {
     * res.setBh08(true); } else if (d.equals("09")) { res.setBh09(true); } else if (d.equals("10")) { res.setBh10(true); } else if
     * (d.equals("11")) { res.setBh11(true); } else if (d.equals("12")) { res.setBh12(true); } else if (d.equals("13")) { res.setBh13(true);
     * } else if (d.equals("14")) { res.setBh14(true); } else if (d.equals("15")) { res.setBh15(true); } else if (d.equals("16")) {
     * res.setBh16(true); } else if (d.equals("17")) { res.setBh17(true); } else if (d.equals("18")) { res.setBh18(true); } else if
     * (d.equals("19")) { res.setBh19(true); } else if (d.equals("20")) { res.setBh20(true); } else if (d.equals("21")) { res.setBh21(true);
     * } else if (d.equals("22")) { res.setBh22(true); } else if (d.equals("23")) { res.setBh23(true); } } // BYMINUTE for (String d :
     * r.getBYMINUTE().split(",")) { switch (d) { case "00": res.setBn00(true); break; case "01": res.setBn01(true); break; case "02":
     * res.setBn02(true); break; case "03": res.setBn03(true); break; case "04": res.setBn04(true); break; case "05": res.setBn05(true);
     * break; case "06": res.setBn06(true); break; case "07": res.setBn07(true); break; case "08": res.setBn08(true); break; case "09":
     * res.setBn09(true); break; case "10": res.setBn10(true); break; case "11": res.setBn11(true); break; case "12": res.setBn12(true);
     * break; case "13": res.setBn13(true); break; case "14": res.setBn14(true); break; case "15": res.setBn15(true); break; case "16":
     * res.setBn16(true); break; case "17": res.setBn17(true); break; case "18": res.setBn18(true); break; case "19": res.setBn19(true);
     * break; case "20": res.setBn20(true); break; case "21": res.setBn21(true); break; case "22": res.setBn22(true); break; case "23":
     * res.setBn23(true); break; case "24": res.setBn24(true); break; case "25": res.setBn25(true); break; case "26": res.setBn26(true);
     * break; case "27": res.setBn27(true); break; case "28": res.setBn28(true); break; case "29": res.setBn29(true); break; case "30":
     * res.setBn30(true); break; case "31": res.setBn31(true); break; case "32": res.setBn32(true); break; case "33": res.setBn33(true);
     * break; case "34": res.setBn34(true); break; case "35": res.setBn35(true); break; case "36": res.setBn36(true); break; case "37":
     * res.setBn37(true); break; case "38": res.setBn38(true); break; case "39": res.setBn39(true); break; case "40": res.setBn40(true);
     * break; case "41": res.setBn41(true); break; case "42": res.setBn42(true); break; case "43": res.setBn43(true); break; case "44":
     * res.setBn44(true); break; case "45": res.setBn45(true); break; case "46": res.setBn46(true); break; case "47": res.setBn47(true);
     * break; case "48": res.setBn48(true); break; case "49": res.setBn49(true); break; case "50": res.setBn50(true); break; case "51":
     * res.setBn51(true); break; case "52": res.setBn52(true); break; case "53": res.setBn53(true); break; case "54": res.setBn54(true);
     * break; case "55": res.setBn55(true); break; case "56": res.setBn56(true); break; case "57": res.setBn57(true); break; case "58":
     * res.setBn58(true); break; case "59": res.setBn59(true); break;
     * 
     * } } return res; }
     */

    public static DTORunLog getDTORunLog(RunLog rl)
    {
        DTORunLog res = new DTORunLog();
        res.setId(rl.getId());
        res.setActiveNodeName(rl.getActiveNodeName());
        res.setApplicationName(rl.getApplicationName());
        res.setBeganRunningAt(rl.getBeganRunningAt().toDate());
        res.setCalendarName(rl.getCalendarName());
        res.setCalendarOccurrence(rl.getCalendarOccurrence());
        res.setChainLev1Name(rl.getChainLev1Name());
        res.setChainName(rl.getChainName());
        res.setDataIn(rl.getDataIn());
        res.setDataOut(rl.getDataOut());
        res.setDns(rl.getDns());
        res.setEnteredPipeAt(rl.getEnteredPipeAt().toDate());
        res.setExecutionNodeName(rl.getExecutionNodeName());
        res.setLastKnownStatus(rl.getLastKnownStatus());
        res.setMarkedForRunAt(rl.getMarkedForUnAt().toDate());
        res.setOsAccount(rl.getOsAccount());
        res.setPlaceName(rl.getPlaceName());
        res.setResultCode(rl.getResultCode());
        res.setSequence(rl.getSequence());
        res.setStoppedRunningAt(rl.getStoppedRunningAt() == null ? null : rl.getStoppedRunningAt().toDate());
        res.setWhatWasRun(rl.getWhatWasRun());
        res.setChainLaunchId(rl.getChainLaunchId().toString());
        res.setShortLog(rl.getShortLog());

        return res;
    }

    public static DTOFunctionalSequence getFunctionalSequence(FunctionalSequence c)
    {
        DTOFunctionalSequence res = new DTOFunctionalSequence(c.getName(), c.getDescription());
        res.setAlertThreshold(c.getAlertThreshold());
        res.setId(c.getId());

        for (FunctionalOccurrence day : c.getOccurrences())
        {
            DTOSequenceOccurrence cd = new DTOSequenceOccurrence(day.getLabel());
            cd.setId(day.getId());
            res.getDays().add(cd);
        }

        return res;
    }

    public static DTOEnvironment getEnvironment(Environment n)
    {
        DTOEnvironment res = new DTOEnvironment();

        List<DTOPlace> places = new ArrayList<>();
        for (Place p : n.getPlacesList())
        {
            places.add(getPlace(p));
        }
        res.setPlaces(places);

        List<DTOExecutionNode> nodes = new ArrayList<>();
        for (ExecutionNode en : n.getNodesList())
        {
            nodes.add(getExecutionNode(en));
        }
        res.setNodes(nodes);

        return res;
    }
}
