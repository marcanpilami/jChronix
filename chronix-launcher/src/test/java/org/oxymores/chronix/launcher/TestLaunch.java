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
        HttpGet rq = new HttpGet("http://localhost:9000/chain.js");
        HttpClient cl = HttpClients.createDefault();
        HttpResponse rs = null;
        /*try
         {
         rs = cl.execute(rq);
         }
         catch (Exception e)
         {
         log.error(e);
         Assert.fail("could not fetch");
         }*/
        // Stop
        Scheduler.handler.stopEngine();
        Scheduler.handler.waitForStopEnd();

        // Test
        if (rs != null)
        {
            Assert.assertEquals(200, rs.getStatusLine().getStatusCode());
        }
    }
}
