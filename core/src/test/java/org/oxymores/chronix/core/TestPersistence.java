/**
 * @author Marc-Antoine Gouillart
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

package org.oxymores.chronix.core;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestPersistence extends TestCase {

	public void testApp() {
		Application a1 = org.oxymores.chronix.demo.DemoApplication
				.getNewDemoApplication();

		// Test connections between objects
		Assert.assertEquals(1, a1.getNodesList().get(0).getCanSendTo().size());
		Assert.assertEquals(1, a1.getNodesList().get(1).getCanReceiveFrom()
				.size());
		Assert.assertEquals(2, a1.getPlacesList().get(0).getMemberOfGroups()
				.size());
		Assert.assertEquals(2, a1.getPlacesList().get(1).getMemberOfGroups()
				.size());
		Assert.assertEquals(4, a1.getGroupsList().get(0).getPlaces().size()
				+ a1.getGroupsList().get(1).getPlaces().size()
				+ a1.getGroupsList().get(2).getPlaces().size());

		// Serialization
		try {
			// Serialize
			Loader.ser2(a1, "C:\\TEMP\\meuh.txt");

			// Deserialization
			Application a2 = Loader.deSerialize("C:\\TEMP\\meuh.txt");

			// Test
			Assert.assertEquals(a1.getNodesList().size(), a2.getNodes().size());
			Assert.assertEquals(a1.getPlacesList().size(), a2.getPlaces()
					.size());
			Assert.assertEquals(a1.getActiveElements().size(), a2
					.getActiveElements().size());
			Assert.assertEquals(a1.getGroupsList().size(), a2.getGroupsList()
					.size());

			Assert.assertEquals(a1.getGroupsList().get(0).getName(), a1
					.getGroupsList().get(0).getName());
		} catch (Exception e) {
			System.err.println("meuh" + e.getMessage() + e);
		}

	}

	public void testContext() {
		ChronixContext c1 = new ChronixContext();
		c1.configurationDirectory = new File("C:\\TEMP\\db");

		// Clear test db directory
		File[] fileList = c1.configurationDirectory.listFiles();
		for (int i = 0; i < fileList.length; i++)
			fileList[i].delete();

		// Create test application and save it inside context
		Application a1 = org.oxymores.chronix.demo.DemoApplication
				.getNewDemoApplication();

		// Init creation of app
		try {
			c1.saveApplication(a1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			c1.setWorkingAsCurrent(a1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// Modification and auto archiving of previous version
		try {
			c1.saveApplication(a1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			c1.setWorkingAsCurrent(a1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// And again, so as to have two previous versions
		try {
			c1.saveApplication(a1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			c1.setWorkingAsCurrent(a1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// Load a context as the engine would
		ChronixContext c2 = ChronixContext.loadContext("C:\\TEMP\\db");

		// Retrieve application
		Application a2 = c2.applicationsById.get(a1.getId());

		// Test
		Assert.assertEquals(a1.getNodes().size(), a2.getNodes().size());
		Assert.assertEquals(a1.getPlaces().size(), a2.getPlaces().size());
		Assert.assertEquals(a1.getActiveElements().size(), a2
				.getActiveElements().size());
		Assert.assertEquals(a1.getGroups().size(), a2.getGroups().size());

		PlaceGroup pg_1 = a1.getGroupsList().get(0);
		PlaceGroup pg_2 = a2.getPlaceGroup(pg_1.id);
		Assert.assertEquals(pg_1.getName(),pg_2.getName());
		// System.out.println(a2.getElements().size());
	}
}
