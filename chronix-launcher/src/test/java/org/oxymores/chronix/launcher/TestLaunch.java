package org.oxymores.chronix.launcher;

import java.text.ParseException;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class TestLaunch
{
    @Test
    public void testLaunch() throws ParseException
    {
        try
        {
            Scheduler.main(null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // Test a fetch
        HttpGet rq = new HttpGet("http://localhost:9000/chain.js");
        HttpClient cl = HttpClients.createDefault();
        HttpResponse rs = null;
        try
        {
            rs = cl.execute(rq);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // Stop
        Scheduler.handler.stopEngine();
        Scheduler.handler.waitForStopEnd();

        // Test
        Assert.assertEquals(200, rs.getStatusLine().getStatusCode());
    }
}
