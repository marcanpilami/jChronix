package org.oxymores.chronix.engine;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.exceptions.IncorrectConfigurationException;

public class TestStart {
	private static Logger log = Logger.getLogger(TestStart.class);

	public static String db1;

	@Before
	public void init() throws Exception {
		db1 = "C:\\TEMP\\db1";

	}

	@Test
	public void testNoDb() throws Exception {
		log.info("***** Test: without a db, the scheduler fails with a adequate exception");
		try {
			ChronixEngine e = new ChronixEngine("C:\\WONTEXISTEVER");
			e.start();
		} catch (IncorrectConfigurationException e) {
			return;
		}
		Assert.fail(); // engine should not have been able to start
	}

	@Test
	public void testCreateAutoApplications() throws Exception {
		log.info("***** Test: with an empty db, the scheduler creates two auto applications");

		ChronixEngine e = new ChronixEngine("C:\\TEMP\\db1");
		e.emptyDb();
		e.injectListenerConfigIntoDb();
		e.start();
		e.stop();

		Assert.assertEquals(2, e.ctx.applicationsById.values().size());

		Application a1 = e.ctx.applicationsByName.get("Operations");
		Application a2 = e.ctx.applicationsByName.get("Chronix Maintenance");

		Assert.assertEquals(1, a1.getChains().size());
		Assert.assertEquals(1, a2.getChains().size());
		Assert.assertEquals(5, a2.getChains().get(0).getStates().size());
	}
}
