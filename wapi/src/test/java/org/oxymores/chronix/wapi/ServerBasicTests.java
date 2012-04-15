package org.oxymores.chronix.wapi;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ServerBasicTests extends TestCase {

	public void testStartStop() throws InterruptedException
	{
		// Create server
		JettyServer server = new JettyServer();
		
		// Start server
		server.start();
		Thread.sleep(2000);  // Starting a server is NOT immediate.
		
		// Call a "ping" service function
		ClientClient client = new ClientClient();
		String res = client.sayHello();
		
		// Check result
		Assert.assertEquals("houba hop", res);
		
		// Stop server.
		server.stop();
	}
}
