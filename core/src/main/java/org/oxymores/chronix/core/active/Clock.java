package org.oxymores.chronix.core.active;

import java.text.ParseException;
import java.util.ArrayList;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.engine.TestStart;

public class Clock extends ActiveNodeBase {
	private static final long serialVersionUID = -5203055591135192345L;
	private static Logger log = Logger.getLogger(Clock.class);

	DateTime CREATED;
	int DURATION;

	ArrayList<ClockRRule> rulesADD, rulesEXC;

	public Clock() {
		rulesADD = new ArrayList<ClockRRule>();
		rulesEXC = new ArrayList<ClockRRule>();
		CREATED = DateTime.now();
	}

	public VEvent getEvent() throws ParseException {
		VEvent evt = new VEvent(new Date(this.CREATED.toDate())/*
																 * , new Dur(0,
																 * 0,
																 * this.DURATION
																 * , 0)
																 */, this.name);

		for (ClockRRule r : rulesADD) {
			evt.getProperties().add(new RRule(r.getRecur()));
		}
		for (ClockRRule r : rulesEXC) {
			evt.getProperties().add(new ExRule(r.getRecur()));
		}

		return evt;
	}

	public int getDURATION() {
		return DURATION;
	}

	public void setDURATION(int dURATION) {
		DURATION = dURATION;
	}

	public DateTime getCREATED() {
		return CREATED;
	}

	@Override
	public boolean visibleInHistory() {
		return false;
	}

	public void addRRuleADD(ClockRRule rule) {
		if (!rulesADD.contains(rule))
			rulesADD.add(rule);
	}

	public void removeRRuleADD(ClockRRule rule) {
		if (rulesADD.contains(rule))
			rulesADD.remove(rule);
	}

	public PeriodList getOccurrences(java.util.Date start, java.util.Date end)
			throws ParseException {
		net.fortuna.ical4j.model.DateTime from = new net.fortuna.ical4j.model.DateTime(
				start);
		net.fortuna.ical4j.model.DateTime to = new net.fortuna.ical4j.model.DateTime(
				end);
		log.debug(String
				.format("Computing occurrences from %s to %s", from, to));
		Period p = new Period(from, to);
		VEvent evt = this.getEvent();
		PeriodList res = evt.calculateRecurrenceSet(p);
		return res;
	}
}
