package org.oxymores.chronix.wapi;

import java.util.UUID;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
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

        Or or = new Or();
        or.setId(UUID.fromString(d.getOrId()));
        or.setName("OR");
        a.addActiveElement(or);

        And and = new And();
        and.setId(UUID.fromString(d.getAndId()));
        and.setName("AND");
        a.addActiveElement(and);

        ChainStart start = new ChainStart();
        start.setId(UUID.fromString(d.getStartId()));
        start.setName("START");
        a.addActiveElement(start);

        ChainEnd end = new ChainEnd();
        end.setId(UUID.fromString(d.getEndId()));
        end.setName("END");
        a.addActiveElement(end);

        for (DTOExecutionNode e : d.getNodes())
            a.addNode(getExecutionNode(e, a));

        for (DTOExecutionNode e : d.getNodes())
            setExecutionNodeNetwork(e, a);

        for (DTOPlace e : d.getPlaces())
            a.addPlace(getPlace(e, a));

        for (DTOPlaceGroup e : d.getGroups())
            a.addGroup(getPlaceGroup(e, a));

        for (DTORRule e : d.getRrules())
            a.addRRule(getRRule(e));

        for (DTOCalendar e : d.getCalendars())
            a.addCalendar(getCalendar(e, a));

        for (DTOShellCommand e : d.getShells())
            a.addActiveElement(getShellCommand(e));

        for (DTOClock e : d.getClocks())
            a.addActiveElement(getClock(e, a));

        for (DTOExternal e : d.getExternals())
            a.addActiveElement(getExternal(e));

        for (DTONextOccurrence e : d.getCalnexts())
            a.addActiveElement(getNextOccurrence(e, a));

        for (DTOChain e : d.getChains())
            a.addActiveElement(getChain(e, a, d));

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
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getAndId())));
                if (s.isOr())
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getOrId())));
                if (s.isStart())
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getStartId())));
                if (s.isEnd())
                    z.setRepresents(a.getActiveNode(UUID.fromString(da.getEndId())));
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
                z.setGuard4(UUID.fromString(s.getGuard4()));
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
            new CalendarDay(dd.getSeq(), r);
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
            r.addRRuleADD(a.getRRule(UUID.fromString(s)));

        for (String s : d.getRulesEXC())
            r.addRRuleEXC(a.getRRule(UUID.fromString(s)));

        return r;
    }

    public static ExecutionNode getExecutionNode(DTOExecutionNode d, Application a)
    {
        ExecutionNode r = new ExecutionNode();
        r.setConsole(d.isConsole);
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

    public static void setExecutionNodeNetwork(DTOExecutionNode d, Application a)
    {
        ExecutionNode r = a.getNode(UUID.fromString(d.getId()));

        for (String s : d.getFromTCP())
        {
            ExecutionNode remote = a.getNode(UUID.fromString(s));
            if (!d.isSimpleRunner)
                remote.connectTo(r, NodeConnectionMethod.TCP);
            else
                remote.connectTo(r, NodeConnectionMethod.RCTRL);
        }

    }

    public static Place getPlace(DTOPlace d, Application a)
    {
        Place r = new Place();
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());
        r.setNode(a.getNode(UUID.fromString(d.getNodeid())));
        r.setProperty1(d.getProp1());
        r.setProperty2(d.getProp2());
        r.setProperty3(d.getProp3());
        r.setProperty4(d.getProp4());
        return r;
    }

    public static PlaceGroup getPlaceGroup(DTOPlaceGroup d, Application a)
    {
        PlaceGroup r = new PlaceGroup();
        r.setDescription(d.getDescription());
        r.setId(UUID.fromString(d.getId()));
        r.setName(d.getName());

        for (String s : d.getPlaces())
        {
            r.addPlace(a.getPlace(UUID.fromString(s)));
        }

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
        res.setId(UUID.fromString(r.id));
        res.setName(r.name);
        res.setDescription(r.description);

        res.setPeriod(r.period);
        res.setINTERVAL(r.interval);

        // ByDay
        String BD = "";
        if (r.bd_01)
            BD += "MO,";
        if (r.bd_02)
            BD += "TU,";
        if (r.bd_03)
            BD += "WE,";
        if (r.bd_04)
            BD += "TH,";
        if (r.bd_05)
            BD += "FR,";
        if (r.bd_06)
            BD += "SA,";
        if (r.bd_07)
            BD += "SU,";
        res.setBYDAY(BD);

        // ByMonthDay
        String BMD = "";
        if (r.bmd_01)
            BMD += "01,";
        if (r.bmdn_01)
            BMD += "-01,";
        if (r.bmd_02)
            BMD += "02,";
        if (r.bmdn_02)
            BMD += "-02,";
        if (r.bmd_03)
            BMD += "03,";
        if (r.bmdn_03)
            BMD += "-03,";
        if (r.bmd_04)
            BMD += "04,";
        if (r.bmdn_04)
            BMD += "-04,";
        if (r.bmd_05)
            BMD += "05,";
        if (r.bmdn_05)
            BMD += "-05,";
        if (r.bmd_06)
            BMD += "06,";
        if (r.bmdn_06)
            BMD += "-06,";
        if (r.bmd_07)
            BMD += "07,";
        if (r.bmdn_07)
            BMD += "-07,";
        if (r.bmd_08)
            BMD += "08,";
        if (r.bmdn_08)
            BMD += "-08,";
        if (r.bmd_09)
            BMD += "09,";
        if (r.bmdn_09)
            BMD += "-09,";
        if (r.bmd_10)
            BMD += "10,";
        if (r.bmdn_10)
            BMD += "-10,";
        if (r.bmd_11)
            BMD += "11,";
        if (r.bmdn_11)
            BMD += "-11,";
        if (r.bmd_12)
            BMD += "12,";
        if (r.bmdn_12)
            BMD += "-12,";
        if (r.bmd_13)
            BMD += "13,";
        if (r.bmdn_13)
            BMD += "-13,";
        if (r.bmd_14)
            BMD += "14,";
        if (r.bmdn_14)
            BMD += "-14,";
        if (r.bmd_15)
            BMD += "15,";
        if (r.bmdn_15)
            BMD += "-15,";
        if (r.bmd_16)
            BMD += "16,";
        if (r.bmdn_16)
            BMD += "-16,";
        if (r.bmd_17)
            BMD += "17,";
        if (r.bmdn_17)
            BMD += "-17,";
        if (r.bmd_18)
            BMD += "18,";
        if (r.bmdn_18)
            BMD += "-18,";
        if (r.bmd_19)
            BMD += "19,";
        if (r.bmdn_19)
            BMD += "-19,";
        if (r.bmd_20)
            BMD += "20,";
        if (r.bmdn_20)
            BMD += "-20,";
        if (r.bmd_21)
            BMD += "21,";
        if (r.bmdn_21)
            BMD += "-21,";
        if (r.bmd_22)
            BMD += "22,";
        if (r.bmdn_22)
            BMD += "-22,";
        if (r.bmd_23)
            BMD += "23,";
        if (r.bmdn_23)
            BMD += "-23,";
        if (r.bmd_24)
            BMD += "24,";
        if (r.bmdn_24)
            BMD += "-24,";
        if (r.bmd_25)
            BMD += "25,";
        if (r.bmdn_25)
            BMD += "-25,";
        if (r.bmd_26)
            BMD += "26,";
        if (r.bmdn_26)
            BMD += "-26,";
        if (r.bmd_27)
            BMD += "27,";
        if (r.bmdn_27)
            BMD += "-27,";
        if (r.bmd_28)
            BMD += "28,";
        if (r.bmdn_28)
            BMD += "-28,";
        if (r.bmd_29)
            BMD += "29,";
        if (r.bmdn_29)
            BMD += "-29,";
        if (r.bmd_30)
            BMD += "30,";
        if (r.bmdn_30)
            BMD += "-30,";
        if (r.bmd_31)
            BMD += "31,";
        if (r.bmdn_31)
            BMD += "-31,";
        res.setBYMONTHDAY(BMD);

        // ByMonth
        String BM = "";
        if (r.bm_01)
            BM += "01,";
        if (r.bm_02)
            BM += "02,";
        if (r.bm_03)
            BM += "03,";
        if (r.bm_04)
            BM += "04,";
        if (r.bm_05)
            BM += "05,";
        if (r.bm_06)
            BM += "06,";
        if (r.bm_07)
            BM += "07,";
        if (r.bm_08)
            BM += "08,";
        if (r.bm_09)
            BM += "09,";
        if (r.bm_10)
            BM += "10,";
        if (r.bm_11)
            BM += "11,";
        if (r.bm_12)
            BM += "12,";
        res.setBYMONTH(BM);

        // ByHour
        String BH = "";
        if (r.bh_00)
            BH += "00,";
        if (r.bh_01)
            BH += "01,";
        if (r.bh_02)
            BH += "02,";
        if (r.bh_03)
            BH += "03,";
        if (r.bh_04)
            BH += "04,";
        if (r.bh_05)
            BH += "05,";
        if (r.bh_06)
            BH += "06,";
        if (r.bh_07)
            BH += "07,";
        if (r.bh_08)
            BH += "08,";
        if (r.bh_09)
            BH += "09,";
        if (r.bh_10)
            BH += "10,";
        if (r.bh_11)
            BH += "11,";
        if (r.bh_12)
            BH += "12,";
        if (r.bh_13)
            BH += "13,";
        if (r.bh_14)
            BH += "14,";
        if (r.bh_15)
            BH += "15,";
        if (r.bh_16)
            BH += "16,";
        if (r.bh_17)
            BH += "17,";
        if (r.bh_18)
            BH += "18,";
        if (r.bh_19)
            BH += "19,";
        if (r.bh_20)
            BH += "20,";
        if (r.bh_21)
            BH += "21,";
        if (r.bh_22)
            BH += "22,";
        if (r.bh_23)
            BH += "23,";

        res.setBYHOUR(BH);

        // ByMinute
        String BN = "";
        if (r.bn_00)
            BN += "00,";
        if (r.bn_01)
            BN += "01,";
        if (r.bn_02)
            BN += "02,";
        if (r.bn_03)
            BN += "03,";
        if (r.bn_04)
            BN += "04,";
        if (r.bn_05)
            BN += "05,";
        if (r.bn_06)
            BN += "06,";
        if (r.bn_07)
            BN += "07,";
        if (r.bn_08)
            BN += "08,";
        if (r.bn_09)
            BN += "09,";
        if (r.bn_10)
            BN += "10,";
        if (r.bn_11)
            BN += "11,";
        if (r.bn_12)
            BN += "12,";
        if (r.bn_13)
            BN += "13,";
        if (r.bn_14)
            BN += "14,";
        if (r.bn_15)
            BN += "15,";
        if (r.bn_16)
            BN += "16,";
        if (r.bn_17)
            BN += "17,";
        if (r.bn_18)
            BN += "18,";
        if (r.bn_19)
            BN += "19,";
        if (r.bn_20)
            BN += "20,";
        if (r.bn_21)
            BN += "21,";
        if (r.bn_22)
            BN += "22,";
        if (r.bn_23)
            BN += "23,";
        if (r.bn_24)
            BN += "24,";
        if (r.bn_25)
            BN += "25,";
        if (r.bn_26)
            BN += "26,";
        if (r.bn_27)
            BN += "27,";
        if (r.bn_28)
            BN += "28,";
        if (r.bn_29)
            BN += "29,";
        if (r.bn_30)
            BN += "30,";
        if (r.bn_31)
            BN += "31,";
        if (r.bn_32)
            BN += "32,";
        if (r.bn_33)
            BN += "33,";
        if (r.bn_34)
            BN += "34,";
        if (r.bn_35)
            BN += "35,";
        if (r.bn_36)
            BN += "36,";
        if (r.bn_37)
            BN += "37,";
        if (r.bn_38)
            BN += "38,";
        if (r.bn_39)
            BN += "39,";
        if (r.bn_40)
            BN += "40,";
        if (r.bn_41)
            BN += "41,";
        if (r.bn_42)
            BN += "42,";
        if (r.bn_43)
            BN += "43,";
        if (r.bn_44)
            BN += "44,";
        if (r.bn_45)
            BN += "45,";
        if (r.bn_46)
            BN += "46,";
        if (r.bn_47)
            BN += "47,";
        if (r.bn_48)
            BN += "48,";
        if (r.bn_49)
            BN += "49,";
        if (r.bn_50)
            BN += "50,";
        if (r.bn_51)
            BN += "51,";
        if (r.bn_52)
            BN += "52,";
        if (r.bn_53)
            BN += "53,";
        if (r.bn_54)
            BN += "54,";
        if (r.bn_55)
            BN += "55,";
        if (r.bn_56)
            BN += "56,";
        if (r.bn_57)
            BN += "57,";
        if (r.bn_58)
            BN += "58,";
        if (r.bn_59)
            BN += "59,";
        res.setBYMINUTE(BN);

        return res;
    }
}
