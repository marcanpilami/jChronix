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

package org.oxymores.chronix.core.active;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.helpers.SenderHelpers;

public class NextOccurrence extends ActiveNodeBase
{
    private static final long serialVersionUID = -2717237089393749264L;
    private static Logger log = Logger.getLogger(NextOccurrence.class);

    @NotNull
    Calendar updatedCalendar;

    public Calendar getUpdatedCalendar()
    {
        return updatedCalendar;
    }

    public void setUpdatedCalendar(Calendar updatedCalendar)
    {
        this.updatedCalendar = updatedCalendar;
    }

    @Override
    public void internalRun(EntityManager em, ChronixContext ctx, PipelineJob pj, MessageProducer jmsProducer, Session jmsSession)
    {
        log.debug(String.format("Calendar %s current occurrence will now be updated", updatedCalendar.getName()));

        CalendarPointer cp = updatedCalendar.getCurrentOccurrencePointer(em);
        CalendarDay oldCd = updatedCalendar.getCurrentOccurrence(em);
        CalendarDay newCd = updatedCalendar.getOccurrenceAfter(oldCd);
        CalendarDay nextCd = updatedCalendar.getOccurrenceAfter(newCd);

        log.info(String.format("Calendar %s will go from %s to %s", updatedCalendar.getName(), oldCd.getValue(), newCd.getValue()));

        if (this.updatedCalendar.warnNotEnoughOccurrencesLeft(em) && !this.updatedCalendar.errorNotEnoughOccurrencesLeft(em))
        {
            log.warn(String.format("Calendar %s will soon reach its end: add more occurrences to it", updatedCalendar.getName()));
        }
        else if (this.updatedCalendar.errorNotEnoughOccurrencesLeft(em))
        {
            log.error(String.format("Calendar %s is nearly at its end: add more occurrences to it", updatedCalendar.getName()));
        }

        cp.setLastEndedOccurrenceCd(newCd);
        cp.setLastEndedOkOccurrenceCd(newCd);
        cp.setLastStartedOccurrenceCd(newCd);
        cp.setNextRunOccurrenceCd(nextCd);

        try
        {
            SenderHelpers.sendCalendarPointer(cp, cp.getCalendar(ctx), jmsSession, jmsProducer, true);
        }
        catch (JMSException e)
        {
            log.error("Could not advance calendar to its next occurrence. It will need to be manually changed", e);
        }
    }
}
