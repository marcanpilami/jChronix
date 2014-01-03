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

package org.oxymores.chronix.planbuilder;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;

public final class CalendarBuilder
{
    private static final String OCCURRENCE_DAY_FORMAT = "dd/MM/yyyy";

    private CalendarBuilder()
    {

    }

    public static Calendar buildWorkDayCalendar(Application a, int year)
    {
        Calendar cal1 = new Calendar();
        cal1.setName("Week worked days");
        cal1.setDescription("All days from monday to friday for the whole year");
        cal1.setManualSequence(false);
        a.addCalendar(cal1);

        DateTime d = new DateTime(year, 1, 1, 0, 0);
        while (d.getYear() == year)
        {
            if (d.getDayOfWeek() <= 5)
            {
                new CalendarDay(d.toString(OCCURRENCE_DAY_FORMAT), cal1);
            }
            d = d.plusDays(1);
        }

        return cal1;
    }

    public static Calendar buildWeekDayCalendar(Application a, int year)
    {
        Calendar cal1 = new Calendar();
        cal1.setName("All days");
        cal1.setDescription("All days for the whole year");
        cal1.setManualSequence(false);
        a.addCalendar(cal1);

        DateTime d = new DateTime(year, 1, 1, 0, 0);
        while (d.getYear() == year)
        {
            new CalendarDay(d.toString(OCCURRENCE_DAY_FORMAT), cal1);
            d = d.plusDays(1);
        }

        return cal1;
    }
}
