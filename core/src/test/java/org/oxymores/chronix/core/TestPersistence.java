package org.oxymores.chronix.core;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestPersistence extends TestCase {

	public void testApp()
	{
		Application a1 = org.oxymores.chronix.demo.DemoApplication.getNewDemoApplication();
		Application a = a1; // Test object
		
		// Test connections between objects
		Assert.assertEquals(1, a.getNodes().get(0).getCanSendTo().size());
		Assert.assertEquals(1, a.getNodes().get(1).getCanReceiveFrom().size());
		Assert.assertEquals(2, a.getPlaces().get(0).getMemberOfGroups().size());
		Assert.assertEquals(2, a.getPlaces().get(1).getMemberOfGroups().size());
		Assert.assertEquals(2, a.getGroups().get(0).getPlaces().size());
		Assert.assertEquals(1, a.getGroups().get(1).getPlaces().size());
		Assert.assertEquals(1, a.getGroups().get(2).getPlaces().size());
		
		// Serialization
		try
		{
			// Serialize
			Loader.ser2(a1, "C:\\TEMP\\meuh.txt");
			
			// Deserialization
			Application a2 = Loader.deSerialize("C:\\TEMP\\meuh.txt");
			
			// Test
			Assert.assertEquals(a1.getNodes().size(), a2.getNodes().size());
			Assert.assertEquals(a1.getPlaces().size(), a2.getPlaces().size());
			Assert.assertEquals(a1.getElements().size(), a2.getElements().size());
			Assert.assertEquals(a1.getGroups().size(), a2.getGroups().size());
		}
		catch (Exception e) {System.err.println("meuh" + e.getMessage() + e);}
		
	}
}
