package org.oxymores.chronix.wapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ConfigurableBase;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.NodeLink;
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
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOCalendar;
import org.oxymores.chronix.dto.DTOCalendarDay;
import org.oxymores.chronix.dto.DTOChain;
import org.oxymores.chronix.dto.DTOClock;
import org.oxymores.chronix.dto.DTOExecutionNode;
import org.oxymores.chronix.dto.DTOExternal;
import org.oxymores.chronix.dto.DTONetwork;
import org.oxymores.chronix.dto.DTONextOccurrence;
import org.oxymores.chronix.dto.DTOParameter;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.DTOShellCommand;
import org.oxymores.chronix.dto.DTOState;
import org.oxymores.chronix.dto.DTOTransition;

public class CoreToDto
{

    public static DTOApplication getApplication(Application a)
    {
        DTOApplication res = new DTOApplication();

        res.setId(a.getId().toString());
        res.setName(a.getName());
        res.setDescription(a.getDescription());

        res.setChains(new ArrayList<DTOChain>());
        res.setPlans(new ArrayList<DTOChain>());
        res.setShells(new ArrayList<DTOShellCommand>());
        res.setGroups(new ArrayList<DTOPlaceGroup>());
        res.setParameters(new ArrayList<DTOParameter>());
        res.setRrules(new ArrayList<DTORRule>());
        res.setClocks(new ArrayList<DTOClock>());
        res.setExternals(new ArrayList<DTOExternal>());
        res.setCalendars(new ArrayList<DTOCalendar>());
        res.setCalnexts(new ArrayList<DTONextOccurrence>());
        res.setVersion(a.getVersion());

        // Unique elements
        for (ConfigurableBase nb : a.getActiveElements().values())
        {
            if (nb instanceof ChainStart)
            {
                res.setStartId(nb.getId().toString());
                break;
            }
        }
        for (ConfigurableBase nb : a.getActiveElements().values())
        {
            if (nb instanceof ChainEnd)
            {
                res.setEndId(nb.getId().toString());
                break;
            }
        }
        for (ConfigurableBase nb : a.getActiveElements().values())
        {
            if (nb instanceof Or)
            {
                res.setOrId(nb.getId().toString());
                break;
            }
        }
        for (ConfigurableBase nb : a.getActiveElements().values())
        {
            if (nb instanceof And)
            {
                res.setAndId(nb.getId().toString());
                break;
            }
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
        for (Calendar c : a.getCalendars())
        {
            res.getCalendars().add(getCalendar(c));
        }

        // Clocks
        for (ClockRRule r : a.getRRulesList())
        {
            res.getRrules().add(getRRule(r));
        }

        Comparator<ActiveNodeBase> comparator_act = new Comparator<ActiveNodeBase>()
        {
            public int compare(ActiveNodeBase c1, ActiveNodeBase c2)
            {
                return c1.getName().compareToIgnoreCase(c2.getName());
            }
        };

        // All the active elements!
        List<ActiveNodeBase> active = new ArrayList<>(a.getActiveElements().values());
        Collections.sort(active, comparator_act);
        for (ActiveNodeBase o : active)
        {
            if (o instanceof Chain)
            {
                Chain c = (Chain) o;
                if (c.isPlan())
                {
                    res.getPlans().add(getChain(c));
                }
                else
                {
                    res.getChains().add(getChain(c));
                }
            }

            if (o instanceof Clock)
            {
                Clock c = (Clock) o;
                res.getClocks().add(getClock(c));
            }

            if (o instanceof ShellCommand)
            {
                ShellCommand s = (ShellCommand) o;
                DTOShellCommand d = new DTOShellCommand();
                d.setId(s.getId().toString());
                d.setCommand(s.getCommand());
                d.setName(s.getName());
                d.setDescription(s.getDescription());
                res.getShells().add(d);
            }

            if (o instanceof External)
            {
                External e = (External) o;
                DTOExternal d = new DTOExternal();
                d.setId(e.getId().toString());
                d.setAccountRestriction(e.getAccountRestriction());
                d.setMachineRestriction(e.getMachineRestriction());
                d.setRegularExpression(e.getRegularExpression());
                d.setName(e.getName());
                d.setDescription(e.getDescription());
                res.getExternals().add(d);
            }

            if (o instanceof NextOccurrence)
            {
                NextOccurrence e = (NextOccurrence) o;
                DTONextOccurrence d = new DTONextOccurrence();
                d.setId(e.getId().toString());
                d.setName(e.getName());
                d.setDescription(e.getDescription());
                d.setCalendarId(e.getUpdatedCalendar().getId().toString());
                res.getCalnexts().add(d);
            }
        }

        return res;
    }

    public static DTOChain getChain(Chain c)
    {
        DTOChain res = new DTOChain();
        res.setId(c.getId().toString());
        res.setName(c.getName());
        res.setDescription(c.getDescription());

        for (State s : c.getStates())
        {
            DTOState t = new DTOState();
            t.setParallel(s.getParallel());
            t.setId(s.getId().toString());
            t.setX(s.getX());
            t.setY(s.getY());
            t.setLabel(s.getRepresents().getName());
            t.setRepresentsId(s.getRepresents().getId().toString());
            if (s.getCalendar() != null)
            {
                t.setCalendarId(s.getCalendar().getId().toString());
                t.setCalendarShift(s.getCalendarShift());
            }
            try
            {
                t.setRunsOnName(s.getRunsOn().getName());
                t.setRunsOnId(s.getRunsOn().getId().toString());
            }
            catch (Exception e)
            {
            }
            if (s.getRepresents() instanceof ChainStart)
            {
                t.setCanReceiveLink(false);
                t.setStart(true);
            }
            if (s.getRepresents() instanceof ChainEnd)
            {
                t.setCanEmitLinks(false);
                t.setEnd(true);
            }
            if (s.getRepresents() instanceof ChainEnd || s.getRepresents() instanceof ChainStart)
            {
                t.setCanBeRemoved(false);
            }
            if (s.getRepresents() instanceof And || s.getRepresents() instanceof Or)
            {
                t.setCanReceiveMultipleLinks(true);
            }
            if (s.getRepresents() instanceof And)
            {
                t.setAnd(true);
            }
            if (s.getRepresents() instanceof Or)
            {
                t.setOr(true);
            }

            res.addState(t);
        }

        for (Transition o : c.getTransitions())
        {
            DTOTransition d = new DTOTransition();
            d.setId(o.getId().toString());
            d.setFrom(o.getStateFrom().getId().toString());
            d.setTo(o.getStateTo().getId().toString());
            d.setGuard1(o.getGuard1());
            d.setGuard2(o.getGuard2());
            d.setGuard3(o.getGuard3());
            d.setGuard4((o.getGuard4() == null ? "" : o.getGuard4().toString()));
            d.setCalendarAware(o.isCalendarAware());
            d.setCalendarShift(o.getCalendarShift());

            res.addTransition(d);
        }

        return res;
    }

    public static DTOExecutionNode getExecutionNode(ExecutionNode en)
    {
        DTOExecutionNode res = new DTOExecutionNode();
        res.setId(en.getId().toString());
        res.setCertFilePath(en.getSshKeyFilePath());
        res.setDns(en.getDns());
        res.setConsole(en.isConsole());
        res.setJmxPort(en.getJmxPort());
        res.setOspassword(en.getOspassword());
        res.setOsusername(en.getOsusername());
        res.setqPort(en.getqPort());
        res.setRemoteExecPort(en.getRemoteExecPort());
        res.setWsPort(en.getWsPort());
        res.setX(en.getX());
        res.setY(en.getY());
        res.setName(en.getName());

        for (NodeLink nl : en.getCanSendTo())
        {
            if (nl.getMethod() == NodeConnectionMethod.TCP)
            {
                res.addToTcp(nl.getNodeTo().getId());
            }
            if (nl.getMethod() == NodeConnectionMethod.RCTRL)
            {
                res.addToRctrl(nl.getNodeTo().getId());
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
        DTOPlace res = new DTOPlace();
        res.setId(p.getId().toString());
        res.setName(p.getName());
        res.setNodeid(p.getNode().getId().toString());
        res.setProp1(p.getProperty1());
        res.setProp2(p.getProperty2());
        res.setProp3(p.getProperty3());
        res.setProp4(p.getProperty4());

        for (UUID pg : p.getMemberOfIds())
        {
            res.addMemberOfGroup(pg);
        }
        return res;
    }

    public static DTOPlaceGroup getPlaceGroup(PlaceGroup g)
    {
        DTOPlaceGroup res = new DTOPlaceGroup();
        res.setDescription(g.getDescription());
        res.setId(g.getId().toString());
        res.setName(g.getName());

        return res;
    }

    public static DTOClock getClock(Clock c)
    {
        DTOClock res = new DTOClock();
        res.setDescription(c.getDescription());
        res.setId(c.getId().toString());
        res.setName(c.getName());

        for (ClockRRule r : c.getRulesADD())
        {
            res.addRuleAdd(r.getId());
        }
        for (ClockRRule r : c.getRulesEXC())
        {
            res.addRuleExc(r.getId());
        }

        return res;
    }

    public static DTORRule getRRule(ClockRRule r)
    {
        DTORRule res = new DTORRule();

        // Identification
        res.setId(r.getId().toString());
        res.setName(r.getName());
        res.setDescription(r.getDescription());

        // Period
        res.setPeriod(r.getPeriod());
        res.setInterval(r.getINTERVAL());

        // ByDay
        for (String d : r.getBYDAY().split(","))
        {
            if (d.equals("MO"))
            {
                res.setBd01(true);
            }
            else if (d.equals("TU"))
            {
                res.setBd02(true);
            }
            else if (d.equals("WE"))
            {
                res.setBd03(true);
            }
            else if (d.equals("TH"))
            {
                res.setBd04(true);
            }
            else if (d.equals("FR"))
            {
                res.setBd05(true);
            }
            else if (d.equals("SA"))
            {
                res.setBd06(true);
            }
            else if (d.equals("SU"))
            {
                res.setBd07(true);
            }
        }
        // ByMonthDay
        for (String d : r.getBYMONTHDAY().split(","))
        {
            if (d.equals("01"))
            {
                res.setBmd01(true);
            }
            else if (d.equals("-01"))
            {
                res.setBmdn01(true);
            }
            else if (d.equals("02"))
            {
                res.setBmd02(true);
            }
            else if (d.equals("-02"))
            {
                res.setBmdn02(true);
            }
            else if (d.equals("03"))
            {
                res.setBmd03(true);
            }
            else if (d.equals("-03"))
            {
                res.setBmdn03(true);
            }
            else if (d.equals("04"))
            {
                res.setBmd04(true);
            }
            else if (d.equals("-04"))
            {
                res.setBmdn04(true);
            }
            else if (d.equals("05"))
            {
                res.setBmd05(true);
            }
            else if (d.equals("-05"))
            {
                res.setBmdn05(true);
            }
            else if (d.equals("06"))
            {
                res.setBmd06(true);
            }
            else if (d.equals("-06"))
            {
                res.setBmdn06(true);
            }
            else if (d.equals("07"))
            {
                res.setBmd07(true);
            }
            else if (d.equals("-07"))
            {
                res.setBmdn07(true);
            }
            else if (d.equals("08"))
            {
                res.setBmd08(true);
            }
            else if (d.equals("-08"))
            {
                res.setBmdn08(true);
            }
            else if (d.equals("09"))
            {
                res.setBmd09(true);
            }
            else if (d.equals("-09"))
            {
                res.setBmdn09(true);
            }
            else if (d.equals("10"))
            {
                res.setBmd10(true);
            }
            else if (d.equals("-10"))
            {
                res.setBmdn10(true);
            }
            else if (d.equals("11"))
            {
                res.setBmd11(true);
            }
            else if (d.equals("-11"))
            {
                res.setBmdn11(true);
            }
            else if (d.equals("12"))
            {
                res.setBmd12(true);
            }
            else if (d.equals("-12"))
            {
                res.setBmdn12(true);
            }
            else if (d.equals("13"))
            {
                res.setBmd13(true);
            }
            else if (d.equals("-13"))
            {
                res.setBmdn13(true);
            }
            else if (d.equals("14"))
            {
                res.setBmd14(true);
            }
            else if (d.equals("-14"))
            {
                res.setBmdn14(true);
            }
            else if (d.equals("15"))
            {
                res.setBmd15(true);
            }
            else if (d.equals("-15"))
            {
                res.setBmdn15(true);
            }
            else if (d.equals("16"))
            {
                res.setBmd16(true);
            }
            else if (d.equals("-16"))
            {
                res.setBmdn16(true);
            }
            else if (d.equals("17"))
            {
                res.setBmd17(true);
            }
            else if (d.equals("-17"))
            {
                res.setBmdn17(true);
            }
            else if (d.equals("18"))
            {
                res.setBmd18(true);
            }
            else if (d.equals("-18"))
            {
                res.setBmdn18(true);
            }
            else if (d.equals("19"))
            {
                res.setBmd19(true);
            }
            else if (d.equals("-19"))
            {
                res.setBmdn19(true);
            }
            else if (d.equals("20"))
            {
                res.setBmd20(true);
            }
            else if (d.equals("-20"))
            {
                res.setBmdn20(true);
            }
            else if (d.equals("21"))
            {
                res.setBmd21(true);
            }
            else if (d.equals("-21"))
            {
                res.setBmdn21(true);
            }
            else if (d.equals("22"))
            {
                res.setBmd22(true);
            }
            else if (d.equals("-22"))
            {
                res.setBmdn22(true);
            }
            else if (d.equals("23"))
            {
                res.setBmd23(true);
            }
            else if (d.equals("-23"))
            {
                res.setBmdn23(true);
            }
            else if (d.equals("24"))
            {
                res.setBmd24(true);
            }
            else if (d.equals("-24"))
            {
                res.setBmdn24(true);
            }
            else if (d.equals("25"))
            {
                res.setBmd25(true);
            }
            else if (d.equals("-25"))
            {
                res.setBmdn25(true);
            }
            else if (d.equals("26"))
            {
                res.setBmd26(true);
            }
            else if (d.equals("-26"))
            {
                res.setBmdn26(true);
            }
            else if (d.equals("27"))
            {
                res.setBmd27(true);
            }
            else if (d.equals("-27"))
            {
                res.setBmdn27(true);
            }
            else if (d.equals("28"))
            {
                res.setBmd28(true);
            }
            else if (d.equals("-28"))
            {
                res.setBmdn29(true);
            }
            else if (d.equals("29"))
            {
                res.setBmd29(true);
            }
            else if (d.equals("-29"))
            {
                res.setBmdn29(true);
            }
            else if (d.equals("30"))
            {
                res.setBmd30(true);
            }
            else if (d.equals("-30"))
            {
                res.setBmdn30(true);
            }
            else if (d.equals("31"))
            {
                res.setBmd31(true);
            }
            else if (d.equals("-31"))
            {
                res.setBmdn31(true);
            }
        }
        // ByMonth
        for (String d : r.getBYMONTH().split(","))
        {
            if (d.equals("01"))
            {
                res.setBm01(true);
            }
            else if (d.equals("02"))
            {
                res.setBm02(true);
            }
            else if (d.equals("03"))
            {
                res.setBm03(true);
            }
            else if (d.equals("04"))
            {
                res.setBm04(true);
            }
            else if (d.equals("05"))
            {
                res.setBm05(true);
            }
            else if (d.equals("06"))
            {
                res.setBm06(true);
            }
            else if (d.equals("07"))
            {
                res.setBm07(true);
            }
            else if (d.equals("08"))
            {
                res.setBm08(true);
            }
            else if (d.equals("09"))
            {
                res.setBm09(true);
            }
            else if (d.equals("10"))
            {
                res.setBm10(true);
            }
            else if (d.equals("11"))
            {
                res.setBm11(true);
            }
            else if (d.equals("12"))
            {
                res.setBm12(true);
            }
        }
        // ByHour
        for (String d : r.getBYHOUR().split(","))
        {
            if (d.equals("00"))
            {
                res.setBh00(true);
            }
            else if (d.equals("01"))
            {
                res.setBh01(true);
            }
            else if (d.equals("02"))
            {
                res.setBh02(true);
            }
            else if (d.equals("03"))
            {
                res.setBh03(true);
            }
            else if (d.equals("04"))
            {
                res.setBh04(true);
            }
            else if (d.equals("05"))
            {
                res.setBh05(true);
            }
            else if (d.equals("06"))
            {
                res.setBh06(true);
            }
            else if (d.equals("07"))
            {
                res.setBh07(true);
            }
            else if (d.equals("08"))
            {
                res.setBh08(true);
            }
            else if (d.equals("09"))
            {
                res.setBh09(true);
            }
            else if (d.equals("10"))
            {
                res.setBh10(true);
            }
            else if (d.equals("11"))
            {
                res.setBh11(true);
            }
            else if (d.equals("12"))
            {
                res.setBh12(true);
            }
            else if (d.equals("13"))
            {
                res.setBh13(true);
            }
            else if (d.equals("14"))
            {
                res.setBh14(true);
            }
            else if (d.equals("15"))
            {
                res.setBh15(true);
            }
            else if (d.equals("16"))
            {
                res.setBh16(true);
            }
            else if (d.equals("17"))
            {
                res.setBh17(true);
            }
            else if (d.equals("18"))
            {
                res.setBh18(true);
            }
            else if (d.equals("19"))
            {
                res.setBh19(true);
            }
            else if (d.equals("20"))
            {
                res.setBh20(true);
            }
            else if (d.equals("21"))
            {
                res.setBh21(true);
            }
            else if (d.equals("22"))
            {
                res.setBh22(true);
            }
            else if (d.equals("23"))
            {
                res.setBh23(true);
            }
        }
        // BYMINUTE
        for (String d : r.getBYMINUTE().split(","))
        {
            switch (d)
            {
                case "00":
                    res.setBn00(true);
                    break;
                case "01":
                    res.setBn01(true);
                    break;
                case "02":
                    res.setBn02(true);
                    break;
                case "03":
                    res.setBn03(true);
                    break;
                case "04":
                    res.setBn04(true);
                    break;
                case "05":
                    res.setBn05(true);
                    break;
                case "06":
                    res.setBn06(true);
                    break;
                case "07":
                    res.setBn07(true);
                    break;
                case "08":
                    res.setBn08(true);
                    break;
                case "09":
                    res.setBn09(true);
                    break;
                case "10":
                    res.setBn10(true);
                    break;
                case "11":
                    res.setBn11(true);
                    break;
                case "12":
                    res.setBn12(true);
                    break;
                case "13":
                    res.setBn13(true);
                    break;
                case "14":
                    res.setBn14(true);
                    break;
                case "15":
                    res.setBn15(true);
                    break;
                case "16":
                    res.setBn16(true);
                    break;
                case "17":
                    res.setBn17(true);
                    break;
                case "18":
                    res.setBn18(true);
                    break;
                case "19":
                    res.setBn19(true);
                    break;
                case "20":
                    res.setBn20(true);
                    break;
                case "21":
                    res.setBn21(true);
                    break;
                case "22":
                    res.setBn22(true);
                    break;
                case "23":
                    res.setBn23(true);
                    break;
                case "24":
                    res.setBn24(true);
                    break;
                case "25":
                    res.setBn25(true);
                    break;
                case "26":
                    res.setBn26(true);
                    break;
                case "27":
                    res.setBn27(true);
                    break;
                case "28":
                    res.setBn28(true);
                    break;
                case "29":
                    res.setBn29(true);
                    break;
                case "30":
                    res.setBn30(true);
                    break;
                case "31":
                    res.setBn31(true);
                    break;
                case "32":
                    res.setBn32(true);
                    break;
                case "33":
                    res.setBn33(true);
                    break;
                case "34":
                    res.setBn34(true);
                    break;
                case "35":
                    res.setBn35(true);
                    break;
                case "36":
                    res.setBn36(true);
                    break;
                case "37":
                    res.setBn37(true);
                    break;
                case "38":
                    res.setBn38(true);
                    break;
                case "39":
                    res.setBn39(true);
                    break;
                case "40":
                    res.setBn40(true);
                    break;
                case "41":
                    res.setBn41(true);
                    break;
                case "42":
                    res.setBn42(true);
                    break;
                case "43":
                    res.setBn43(true);
                    break;
                case "44":
                    res.setBn44(true);
                    break;
                case "45":
                    res.setBn45(true);
                    break;
                case "46":
                    res.setBn46(true);
                    break;
                case "47":
                    res.setBn47(true);
                    break;
                case "48":
                    res.setBn48(true);
                    break;
                case "49":
                    res.setBn49(true);
                    break;
                case "50":
                    res.setBn50(true);
                    break;
                case "51":
                    res.setBn51(true);
                    break;
                case "52":
                    res.setBn52(true);
                    break;
                case "53":
                    res.setBn53(true);
                    break;
                case "54":
                    res.setBn54(true);
                    break;
                case "55":
                    res.setBn55(true);
                    break;
                case "56":
                    res.setBn56(true);
                    break;
                case "57":
                    res.setBn57(true);
                    break;
                case "58":
                    res.setBn58(true);
                    break;
                case "59":
                    res.setBn59(true);
                    break;

            }
        }
        return res;
    }

    public static DTORunLog getDTORunLog(RunLog rl)
    {
        DTORunLog res = new DTORunLog();
        res.setId(rl.getId().toString());
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
        res.setStoppedRunningAt(rl.getStoppedRunningAt().toDate());
        res.setWhatWasRun(rl.getWhatWasRun());

        return res;
    }

    public static DTOCalendar getCalendar(Calendar c)
    {
        DTOCalendar res = new DTOCalendar();
        res.setAlertThreshold(c.getAlertThreshold());
        res.setDescription(c.getDescription());
        res.setId(c.getId().toString());
        res.setName(c.getName());
        res.setDays(new ArrayList<DTOCalendarDay>());

        for (CalendarDay day : c.getDays())
        {
            DTOCalendarDay cd = new DTOCalendarDay();
            cd.setId(day.getId().toString());
            cd.setSeq(day.getValue());
            res.getDays().add(cd);
        }

        return res;
    }

    public static DTONetwork getNetwork(Network n)
    {
        DTONetwork res = new DTONetwork();

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
