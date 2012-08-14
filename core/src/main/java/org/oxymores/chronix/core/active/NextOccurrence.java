package org.oxymores.chronix.core.active;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.SenderHelpers;

public class NextOccurrence extends ActiveNodeBase {
	private static final long serialVersionUID = -2717237089393749264L;
	private static Logger log = Logger.getLogger(NextOccurrence.class);

	Calendar updatedCalendar;

	public Calendar getUpdatedCalendar() {
		return updatedCalendar;
	}

	public void setUpdatedCalendar(Calendar updatedCalendar) {
		this.updatedCalendar = updatedCalendar;
	}

	@Override
	public void internalRun(EntityManager em, ChronixContext ctx, PipelineJob pj, MessageProducer jmsProducer, Session jmsSession) {
		log.debug(String.format("Calendar %s current occurrence will now be updated", updatedCalendar.getName()));

		CalendarPointer cp = updatedCalendar.getCurrentOccurrencePointer(em);
		CalendarDay old_cd = updatedCalendar.getCurrentOccurrence(em);
		CalendarDay new_cd = updatedCalendar.getOccurrenceAfter(old_cd);
		CalendarDay next_cd = updatedCalendar.getOccurrenceAfter(new_cd);

		log.info(String.format("Calendar %s will go from %s to %s", updatedCalendar.getName(), old_cd.getValue(), new_cd.getValue()));

		if (this.updatedCalendar.warnNotEnoughOccurrencesLeft(em) && !this.updatedCalendar.errorNotEnoughOccurrencesLeft(em))
			log.warn(String.format("Calendar %s will soon reach its end: add more occurrences to it", updatedCalendar.getName()));
		else if (this.updatedCalendar.errorNotEnoughOccurrencesLeft(em))
			log.error(String.format("Calendar %s is nearly at its end: add more occurrences to it", updatedCalendar.getName()));

		cp.setLastEndedOccurrenceCd(new_cd);
		cp.setLastEndedOkOccurrenceCd(new_cd);
		cp.setLastStartedOccurrenceCd(new_cd);
		cp.setNextRunOccurrenceCd(next_cd);

		try {
			SenderHelpers.sendCalendarPointer(cp, cp.getCalendar(ctx), jmsSession, jmsProducer, true);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
