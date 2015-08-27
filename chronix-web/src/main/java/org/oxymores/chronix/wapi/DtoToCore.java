package org.oxymores.chronix.wapi;

import java.util.UUID;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Transition;
import org.oxymores.chronix.core.active.And;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.Or;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOCalendar;
import org.oxymores.chronix.dto.DTOCalendarDay;
import org.oxymores.chronix.dto.DTOChain;
import org.oxymores.chronix.dto.DTOClock;
import org.oxymores.chronix.dto.DTOExecutionNode;
import org.oxymores.chronix.dto.DTOExternal;
import org.oxymores.chronix.dto.DTONetwork;
import org.oxymores.chronix.dto.DTONextOccurrence;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.DTOShellCommand;
import org.oxymores.chronix.dto.DTOState;
import org.oxymores.chronix.dto.DTOTransition;

public class DtoToCore
{
    public static Application getApplication(DTOApplication d)
    {
        Application a = new Application();
        a.setId(UUID.fromString(d.getId()));
        a.setDescription(d.getDescription());
        a.setname(d.getName());
        a.setVersion(d.getVersion());

        Or or = new Or();
        or.setId(UUID.fromString(d.getOrId()));
        or.setName("OR");
        or.setDescription("OR logical door - unique for the whole application");
        a.addActiveElement(or);

        And and = new And();
        and.setId(UUID.fromString(d.getAndId()));
        and.setName("AND");
        and.setDescription("AND logical door - unique for the whole application");
        a.addActiveElement(and);

        ChainStart start = new ChainStart();
        start.setId(UUID.fromString(d.getStartId()));
        start.setName("START");
        start.setDescription("Marks the beginning of a chain. Can be ignored in global plans");
        a.addActiveElement(start);

        ChainEnd end = new ChainEnd();
        end.setId(UUID.fromString(d.getEndId()));
        end.setName("END");
        end.setDescription("Marks the end of a chain. Can be ignored in global plans");
        a.addActiveElement(end);

        for (DTOPlaceGroup e : d.getGroups())
        {
            a.addGroup(getPlaceGroup(e, a));
        }

        for (DTORRule e : d.getRrules())
        {
            a.addRRule(getRRule(e));
        }

        for (DTOCalendar e : d.getCalendars())
        {
            a.addCalendar(getCalendar(e, a));
        }

        for (DTOShellCommand e : d.getShells())
        {
            a.addActiveElement(getShellCommand(e));
        }

        for (DTOClock e : d.getClocks())
        {
            a.addActiveElement(getClock(e, a));
        }

        for (DTOExternal e : d.getExternals())
        {
            a.addActiveElement(getExternal(e));
        }

        for (DTONextOccurrence e : d.getCalnexts())
        {
            a.addActiveElement(getNextOccurrence(e, a));
        }

        for (DTOChain e : d.getChains())
        {
            a.addActiveElement(getChain(e, a, d));
        }
        for (DTOChain e : d.getPlans())
        {
            a.addActiveElement(getChain(e, a, d));
        }

        return a;
    }

    public static Chain getChain(DTOChain d, Application a, DTOApplication da)
    {
        // Create chain (basics)
        Chain r = new Chain();
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());

        // States
        for (DTOState s : d.getStates())
        {
            State z = new State();
            z.setChain(r);
            z.setApplication(a);
            // z.setEndOfOccurrence(s.get)
            z.setEventValidityMn(s.getEventValidityMn());
            z.setId(UUID.fromString(s.getId()));
            z.setKillAfterMn(s.getKillAfterMn());
            // z.setLoopMissedOccurrences(s.get)
            z.setMaxPipeWaitTime(s.getMaxPipeWaitTime());
            z.setParallel(s.isParallel());
            z.setRunsOn(a.getGroup(UUID.fromString(s.getRunsOnId())));
            // z.setTokens(tokens)
            z.setWarnAfterMn(s.getWarnAfterMn());
            z.setX(s.getX());
            z.setY(s.getY());

            if (s.getCalendarId() != null)
            {
                Calendar c = a.getCalendar(UUID.fromString(s.getCalendarId()));
                z.setCalendar(c);
                z.setCalendarShift(s.getCalendarShift());
            }

            ActiveNodeBase target = a.getActiveNode(UUID.fromString(s.getRepresentsId()));
            if (target != null)
            {
                z.setRepresents(target);
            }
            else
            {
                if (s.isAnd())
                {
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getAndId())));
                }
                if (s.isOr())
                {
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getOrId())));
                }
                if (s.isStart())
                {
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getStartId())));
                }
                if (s.isEnd())
                {
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getEndId())));
                }
            }
        }

        for (DTOTransition s : d.getTransitions())
        {
            Transition z = new Transition();
            z.setApplication(a);
            z.setCalendarAware(s.isCalendarAware());
            z.setCalendarShift(s.getCalendarShift() == null ? 0 : s.getCalendarShift());
            z.setChain(r);
            z.setGuard1(s.getGuard1());
            z.setGuard2(s.getGuard2());
            z.setGuard3(s.getGuard3());
            if ((s.getGuard4() != null) && (!s.getGuard4().isEmpty()))
            {
                z.setGuard4(UUID.fromString(s.getGuard4()));
            }
            z.setId(UUID.fromString(s.getId()));
            z.setStateFrom(r.getState(UUID.fromString(s.getFrom())));
            z.setStateTo(r.getState(UUID.fromString(s.getTo())));
        }

        return r;
    }

    public static Calendar getCalendar(DTOCalendar d, Application a)
    {
        Calendar r = new Calendar();
        r.setAlertThreshold(d.getAlertThreshold());
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());
        r.setApplication(a);

        for (DTOCalendarDay dd : d.getDays())
        {
            CalendarDay calendarDay = new CalendarDay(dd.getSeq(), r);
        }
        return r;
    }

    public static NextOccurrence getNextOccurrence(DTONextOccurrence d, Application a)
    {
        NextOccurrence r = new NextOccurrence();
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());
        r.setUpdatedCalendar(a.getCalendar(UUID.fromString(d.getCalendarId())));

        return r;
    }

    public static External getExternal(DTOExternal d)
    {
        External r = new External();
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());
        r.setRegularExpression(d.getRegularExpression());

        return r;
    }

    public static Clock getClock(DTOClock d, Application a)
    {
        Clock r = new Clock();
        r.setDescription(d.getDescription());
        r.setDURATION(0);
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());

        for (String s : d.getRulesADD())
        {
            r.addRRuleADD(a.getRRule(UUID.fromString(s)));
        }

        for (String s : d.getRulesEXC())
        {
            r.addRRuleEXC(a.getRRule(UUID.fromString(s)));
        }

        return r;
    }

    /**
     * Pass 1: create nodes without transitions
     * @param d the DTO object describing the execution node - a true EN will be created from this.
     * @return the core execution node corresponding to the DTO object.
     */
    public static ExecutionNode getExecutionNode(DTOExecutionNode d)
    {
        ExecutionNode r = new ExecutionNode();
        r.setName(d.getName());
        r.setConsole(d.isConsole());
        r.setDns(d.getDns());
        r.setId(UUID.fromString(d.getId()));
        r.setJmxPort(d.getJmxPort());
        r.setOspassword(d.getOspassword());
        r.setOsusername(d.getOsusername());
        r.setqPort(d.getqPort());
        r.setRemoteExecPort(d.getRemoteExecPort());
        // r.setSshKeyFilePath(d.get)
        // r.setSslKeyFilePath(d.gets)
        // r.setType(type);
        r.setWsPort(d.getWsPort());
        r.setX(d.getX());
        r.setY(d.getY());

        return r;
    }

    /**
     * Pass 2: add connections between nodes. All nodes must exist when this method is called.
     * @param d the DTO object describing the node to connect.
     * @param n the Network that the new connections should be added to.
     */
    public static void setExecutionNodeNetwork(DTOExecutionNode d, Network n)
    {
        ExecutionNode from = n.getNode(UUID.fromString(d.getId()));

        for (String s : d.getToTCP())
        {
            ExecutionNode target = n.getNode(UUID.fromString(s));
            from.connectTo(target, NodeConnectionMethod.TCP);
        }
        for (String s : d.getToRCTRL())
        {
            ExecutionNode target = n.getNode(UUID.fromString(s));
            from.connectTo(target, NodeConnectionMethod.RCTRL);
        }
    }

    public static Place getPlace(DTOPlace d, Network n)
    {
        Place r = new Place();
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());
        r.setNode(n.getNode(UUID.fromString(d.getNodeid())));
        r.setProperty1(d.getProp1());
        r.setProperty2(d.getProp2());
        r.setProperty3(d.getProp3());
        r.setProperty4(d.getProp4());
        for (String s : d.getMemberOf())
        {
            r.addGroupMembership(UUID.fromString(s));
        }
        return r;
    }

    public static PlaceGroup getPlaceGroup(DTOPlaceGroup d, Application a)
    {
        PlaceGroup r = new PlaceGroup();
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());

        // Places <-> Group links are defined inside Places, not Groups.
        return r;
    }

    public static ShellCommand getShellCommand(DTOShellCommand d)
    {
        ShellCommand r = new ShellCommand();
        r.setCommand(d.getCommand());
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());

        return r;
    }

    public static ClockRRule getRRule(DTORRule r)
    {
        ClockRRule res = new ClockRRule();
        res.setId(UUID.fromString(r.getId()));
        res.setName(r.getName());
        res.setDescription(r.getDescription());

        res.setPeriod(r.getPeriod());
        res.setINTERVAL(r.getInterval());

        // ByDay
        String BD = "";
        if (r.isBd01())
        {
            BD += "MO,";
        }
        if (r.isBd02())
        {
            BD += "TU,";
        }
        if (r.isBd03())
        {
            BD += "WE,";
        }
        if (r.isBd04())
        {
            BD += "TH,";
        }
        if (r.isBd05())
        {
            BD += "FR,";
        }
        if (r.isBd06())
        {
            BD += "SA,";
        }
        if (r.isBd07())
        {
            BD += "SU,";
        }
        res.setBYDAY(BD);

        // ByMonthDay
        String BMD = "";
        if (r.isBmd01())
        {
            BMD += "01,";
        }
        if (r.isBmdn01())
        {
            BMD += "-01,";
        }
        if (r.isBmd02())
        {
            BMD += "02,";
        }
        if (r.isBmdn02())
        {
            BMD += "-02,";
        }
        if (r.isBmd03())
        {
            BMD += "03,";
        }
        if (r.isBmdn03())
        {
            BMD += "-03,";
        }
        if (r.isBmd04())
        {
            BMD += "04,";
        }
        if (r.isBmdn04())
        {
            BMD += "-04,";
        }
        if (r.isBmd05())
        {
            BMD += "05,";
        }
        if (r.isBmdn05())
        {
            BMD += "-05,";
        }
        if (r.isBmd06())
        {
            BMD += "06,";
        }
        if (r.isBmdn06())
        {
            BMD += "-06,";
        }
        if (r.isBmd07())
        {
            BMD += "07,";
        }
        if (r.isBmdn07())
        {
            BMD += "-07,";
        }
        if (r.isBmd08())
        {
            BMD += "08,";
        }
        if (r.isBmdn08())
        {
            BMD += "-08,";
        }
        if (r.isBmd09())
        {
            BMD += "09,";
        }
        if (r.isBmdn09())
        {
            BMD += "-09,";
        }
        if (r.isBmd10())
        {
            BMD += "10,";
        }
        if (r.isBmdn10())
        {
            BMD += "-10,";
        }
        if (r.isBmd11())
        {
            BMD += "11,";
        }
        if (r.isBmdn11())
        {
            BMD += "-11,";
        }
        if (r.isBmd12())
        {
            BMD += "12,";
        }
        if (r.isBmdn12())
        {
            BMD += "-12,";
        }
        if (r.isBmd13())
        {
            BMD += "13,";
        }
        if (r.isBmdn13())
        {
            BMD += "-13,";
        }
        if (r.isBmd14())
        {
            BMD += "14,";
        }
        if (r.isBmdn14())
        {
            BMD += "-14,";
        }
        if (r.isBmd15())
        {
            BMD += "15,";
        }
        if (r.isBmdn15())
        {
            BMD += "-15,";
        }
        if (r.isBmd16())
        {
            BMD += "16,";
        }
        if (r.isBmdn16())
        {
            BMD += "-16,";
        }
        if (r.isBmd17())
        {
            BMD += "17,";
        }
        if (r.isBmdn17())
        {
            BMD += "-17,";
        }
        if (r.isBmd18())
        {
            BMD += "18,";
        }
        if (r.isBmdn18())
        {
            BMD += "-18,";
        }
        if (r.isBmd19())
        {
            BMD += "19,";
        }
        if (r.isBmdn19())
        {
            BMD += "-19,";
        }
        if (r.isBmd20())
        {
            BMD += "20,";
        }
        if (r.isBmdn20())
        {
            BMD += "-20,";
        }
        if (r.isBmd21())
        {
            BMD += "21,";
        }
        if (r.isBmdn21())
        {
            BMD += "-21,";
        }
        if (r.isBmd22())
        {
            BMD += "22,";
        }
        if (r.isBmdn22())
        {
            BMD += "-22,";
        }
        if (r.isBmd23())
        {
            BMD += "23,";
        }
        if (r.isBmdn23())
        {
            BMD += "-23,";
        }
        if (r.isBmd24())
        {
            BMD += "24,";
        }
        if (r.isBmdn24())
        {
            BMD += "-24,";
        }
        if (r.isBmd25())
        {
            BMD += "25,";
        }
        if (r.isBmdn25())
        {
            BMD += "-25,";
        }
        if (r.isBmd26())
        {
            BMD += "26,";
        }
        if (r.isBmdn26())
        {
            BMD += "-26,";
        }
        if (r.isBmd27())
        {
            BMD += "27,";
        }
        if (r.isBmdn27())
        {
            BMD += "-27,";
        }
        if (r.isBmd28())
        {
            BMD += "28,";
        }
        if (r.isBmdn28())
        {
            BMD += "-28,";
        }
        if (r.isBmd29())
        {
            BMD += "29,";
        }
        if (r.isBmdn29())
        {
            BMD += "-29,";
        }
        if (r.isBmd30())
        {
            BMD += "30,";
        }
        if (r.isBmdn30())
        {
            BMD += "-30,";
        }
        if (r.isBmd31())
        {
            BMD += "31,";
        }
        if (r.isBmdn31())
        {
            BMD += "-31,";
        }
        res.setBYMONTHDAY(BMD);

        // ByMonth
        String BM = "";
        if (r.isBm01())
        {
            BM += "01,";
        }
        if (r.isBm02())
        {
            BM += "02,";
        }
        if (r.isBm03())
        {
            BM += "03,";
        }
        if (r.isBm04())
        {
            BM += "04,";
        }
        if (r.isBm05())
        {
            BM += "05,";
        }
        if (r.isBm06())
        {
            BM += "06,";
        }
        if (r.isBm07())
        {
            BM += "07,";
        }
        if (r.isBm08())
        {
            BM += "08,";
        }
        if (r.isBm09())
        {
            BM += "09,";
        }
        if (r.isBm10())
        {
            BM += "10,";
        }
        if (r.isBm11())
        {
            BM += "11,";
        }
        if (r.isBm12())
        {
            BM += "12,";
        }
        res.setBYMONTH(BM);

        // ByHour
        String BH = "";
        if (r.isBh00())
        {
            BH += "00,";
        }
        if (r.isBh01())
        {
            BH += "01,";
        }
        if (r.isBh02())
        {
            BH += "02,";
        }
        if (r.isBh03())
        {
            BH += "03,";
        }
        if (r.isBh04())
        {
            BH += "04,";
        }
        if (r.isBh05())
        {
            BH += "05,";
        }
        if (r.isBh06())
        {
            BH += "06,";
        }
        if (r.isBh07())
        {
            BH += "07,";
        }
        if (r.isBh08())
        {
            BH += "08,";
        }
        if (r.isBh09())
        {
            BH += "09,";
        }
        if (r.isBh10())
        {
            BH += "10,";
        }
        if (r.isBh11())
        {
            BH += "11,";
        }
        if (r.isBh12())
        {
            BH += "12,";
        }
        if (r.isBh13())
        {
            BH += "13,";
        }
        if (r.isBh14())
        {
            BH += "14,";
        }
        if (r.isBh15())
        {
            BH += "15,";
        }
        if (r.isBh16())
        {
            BH += "16,";
        }
        if (r.isBh17())
        {
            BH += "17,";
        }
        if (r.isBh18())
        {
            BH += "18,";
        }
        if (r.isBh19())
        {
            BH += "19,";
        }
        if (r.isBh20())
        {
            BH += "20,";
        }
        if (r.isBh21())
        {
            BH += "21,";
        }
        if (r.isBh22())
        {
            BH += "22,";
        }
        if (r.isBh23())
        {
            BH += "23,";
        }

        res.setBYHOUR(BH);

        // ByMinute
        String BN = "";
        if (r.isBn00())
        {
            BN += "00,";
        }
        if (r.isBn01())
        {
            BN += "01,";
        }
        if (r.isBn02())
        {
            BN += "02,";
        }
        if (r.isBn03())
        {
            BN += "03,";
        }
        if (r.isBn04())
        {
            BN += "04,";
        }
        if (r.isBn05())
        {
            BN += "05,";
        }
        if (r.isBn06())
        {
            BN += "06,";
        }
        if (r.isBn07())
        {
            BN += "07,";
        }
        if (r.isBn08())
        {
            BN += "08,";
        }
        if (r.isBn09())
        {
            BN += "09,";
        }
        if (r.isBn10())
        {
            BN += "10,";
        }
        if (r.isBn11())
        {
            BN += "11,";
        }
        if (r.isBn12())
        {
            BN += "12,";
        }
        if (r.isBn13())
        {
            BN += "13,";
        }
        if (r.isBn14())
        {
            BN += "14,";
        }
        if (r.isBn15())
        {
            BN += "15,";
        }
        if (r.isBn16())
        {
            BN += "16,";
        }
        if (r.isBn17())
        {
            BN += "17,";
        }
        if (r.isBn18())
        {
            BN += "18,";
        }
        if (r.isBn19())
        {
            BN += "19,";
        }
        if (r.isBn20())
        {
            BN += "20,";
        }
        if (r.isBn21())
        {
            BN += "21,";
        }
        if (r.isBn22())
        {
            BN += "22,";
        }
        if (r.isBn23())
        {
            BN += "23,";
        }
        if (r.isBn24())
        {
            BN += "24,";
        }
        if (r.isBn25())
        {
            BN += "25,";
        }
        if (r.isBn26())
        {
            BN += "26,";
        }
        if (r.isBn27())
        {
            BN += "27,";
        }
        if (r.isBn28())
        {
            BN += "28,";
        }
        if (r.isBn29())
        {
            BN += "29,";
        }
        if (r.isBn30())
        {
            BN += "30,";
        }
        if (r.isBn31())
        {
            BN += "31,";
        }
        if (r.isBn32())
        {
            BN += "32,";
        }
        if (r.isBn33())
        {
            BN += "33,";
        }
        if (r.isBn34())
        {
            BN += "34,";
        }
        if (r.isBn35())
        {
            BN += "35,";
        }
        if (r.isBn36())
        {
            BN += "36,";
        }
        if (r.isBn37())
        {
            BN += "37,";
        }
        if (r.isBn38())
        {
            BN += "38,";
        }
        if (r.isBn39())
        {
            BN += "39,";
        }
        if (r.isBn40())
        {
            BN += "40,";
        }
        if (r.isBn41())
        {
            BN += "41,";
        }
        if (r.isBn42())
        {
            BN += "42,";
        }
        if (r.isBn43())
        {
            BN += "43,";
        }
        if (r.isBn44())
        {
            BN += "44,";
        }
        if (r.isBn45())
        {
            BN += "45,";
        }
        if (r.isBn46())
        {
            BN += "46,";
        }
        if (r.isBn47())
        {
            BN += "47,";
        }
        if (r.isBn48())
        {
            BN += "48,";
        }
        if (r.isBn49())
        {
            BN += "49,";
        }
        if (r.isBn50())
        {
            BN += "50,";
        }
        if (r.isBn51())
        {
            BN += "51,";
        }
        if (r.isBn52())
        {
            BN += "52,";
        }
        if (r.isBn53())
        {
            BN += "53,";
        }
        if (r.isBn54())
        {
            BN += "54,";
        }
        if (r.isBn55())
        {
            BN += "55,";
        }
        if (r.isBn56())
        {
            BN += "56,";
        }
        if (r.isBn57())
        {
            BN += "57,";
        }
        if (r.isBn58())
        {
            BN += "58,";
        }
        if (r.isBn59())
        {
            BN += "59,";
        }
        res.setBYMINUTE(BN);

        return res;
    }

    public static Network getNetwork(DTONetwork d)
    {
        Network n = new Network();

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
}
