package org.oxymores.chronix.launcher;

import java.text.ParseException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestLaunch
{
    private static final Logger log = Logger.getLogger(TestLaunch.class);

    @Test
    public void testLaunch() throws ParseException
    {
        try
        {
            Scheduler.main(null);
        }
        catch (Exception e)
        {
            log.error("", e);
            Assert.fail("Could not init the scheduler we no arguments");
        }

        // Test a fetch
        HttpGet rq = new HttpGet("http://localhost:1790/js/chain.js");
        HttpClient cl = HttpClients.createDefault();
        HttpResponse rs = null;
        try
        {
            rs = cl.execute(rq);
        }
        catch (Exception e)
        {
            log.error(e);
            Assert.fail("could not fetch");
        }

        rq = new HttpGet("http://localhost:1790/ws/meta/network");
        HttpResponse rs2 = null;
        try
        {
            rs2 = cl.execute(rq);
        }
        catch (Exception e)
        {
            log.error(e);
            Assert.fail("could not fetch");
        }

        // Stop
        Scheduler.stop(null);

        // Test
        if (rs != null || rs2 == null)
        {
            Assert.assertEquals(200, rs.getStatusLine().getStatusCode());
        }
    }
}
