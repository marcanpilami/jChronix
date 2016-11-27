package org.oxymores.chronix.source.basic.prv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.api.source.EventSourceTimedRunResult;
import org.oxymores.chronix.api.source.JobDescription;

public class TestClockRec
{
    private class NewJob implements JobDescription
    {
        private DateTime vtime;
        private String addRules, excRules;

        public NewJob(DateTime startVirtualTime, String addRules, String excRules)
        {
            this.vtime = startVirtualTime;
            this.addRules = addRules;
            this.excRules = excRules;
        }

        @Override
        public UUID getEventSourceId()
        {
            return null;
        }

        @Override
        public UUID getLaunchId()
        {
            return null;
        }

        @Override
        public UUID getScopeId()
        {
            return null;
        }

        @Override
        public UUID getParentContainerLaunchId()
        {
            return null;
        }

        @Override
        public DateTime getVirtualTimeStart()
        {
            return this.vtime;
        }

        @Override
        public boolean isOutOfPlan()
        {
            return false;
        }

        @Override
        public List<Entry<String, String>> getParameters()
        {
            return null;
        }

        @Override
        public Map<String, String> getFields()
        {
            Map<String, String> res = new HashMap<>();
            res.put("eventAddRRule", this.addRules);
            res.put("eventExcRRule", this.excRules);
            return res;
        }

        @Override
        public Map<String, String> getEnvironment()
        {
            return null;
        }

    }

    @Test
    public void testRec1()
    {
        // Stupid single add rule test

        ClockProvider cp = new ClockProvider();

        // 4th of January 2010 was a Monday. ;BYHOUR=0,2
        JobDescription jd = new NewJob(new DateTime(2010, 01, 04, 00, 10), "FREQ=MINUTELY;INTERVAL=2;BYMINUTE=10,20", null);

        EventSourceTimedRunResult res = cp.run(null, jd);
        Assert.assertEquals((int) 0, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 20), res.callMeBackAt);
    }

    @Test
    public void testRec2()
    {
        // Test for first occurrence issue.

        ClockProvider cp = new ClockProvider();

        // 4th of January 2010 was a Monday.
        // Every minute at 10s.
        JobDescription jd = new NewJob(new DateTime(2010, 01, 04, 00, 00), "FREQ=MINUTELY;INTERVAL=1;BYSECOND=10", null);

        // This time, the given virtual time should not be an occurrence. So result should be 2.
        EventSourceTimedRunResult res = cp.run(null, jd);
        Assert.assertEquals((int) 2, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 00, 10), res.callMeBackAt);

        // Now input the returned "call me next" virtual time to the provider. It should be an occurrence => 0.
        res = cp.run(null, new NewJob(res.callMeBackAt, "FREQ=MINUTELY;INTERVAL=1;BYSECOND=10", null));
        Assert.assertEquals((int) 0, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 01, 10), res.callMeBackAt);
    }

    @Test
    public void testRec3()
    {
        // Test with exception rule

        ClockProvider cp = new ClockProvider();

        // 4th of January 2010 was a Monday.
        // Every minute at 0s except pair minutes.
        NewJob jd = new NewJob(new DateTime(2010, 01, 04, 00, 00), "FREQ=MINUTELY;INTERVAL=1;BYSECOND=0",
                "FREQ=MINUTELY;INTERVAL=2;BYSECOND=0");

        // Expected is: 00:01, 00:03, ...
        EventSourceTimedRunResult res = cp.run(null, jd);
        Assert.assertEquals((int) 2, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 01, 00), res.callMeBackAt);

        jd.vtime = res.callMeBackAt;
        res = cp.run(null, jd);
        Assert.assertEquals((int) 0, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 03, 00), res.callMeBackAt);
    }

    @Test
    public void testPrf()
    {
        // Test with exception rule

        ClockProvider cp = new ClockProvider();

        // 4th of January 2010 was a Monday.
        // Every minute at 0s except pair minutes.
        NewJob jd = new NewJob(new DateTime(2010, 01, 04, 00, 00), "FREQ=MINUTELY;INTERVAL=1", null);

        // Expected is: 00:01, 00:03, ...
        EventSourceTimedRunResult res = cp.run(null, jd);
        Assert.assertEquals((int) 2, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 01, 00), res.callMeBackAt);

        jd.vtime = res.callMeBackAt;
        res = cp.run(null, jd);
        Assert.assertEquals((int) 0, (int) res.returnCode);
        Assert.assertEquals(new DateTime(2010, 01, 04, 00, 03, 00), res.callMeBackAt);
    }

}
