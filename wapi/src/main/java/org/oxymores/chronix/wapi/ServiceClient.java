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

package org.oxymores.chronix.wapi;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.demo.DemoApplication;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOChain;
import org.oxymores.chronix.dto.Frontier;
import org.oxymores.chronix.internalapi.IServiceClient;

public class ServiceClient implements IServiceClient {

	private static Logger log = Logger.getLogger(ChronixContext.class);

	@Override
	public String sayHello() {
		log.debug("Ping service was called");
		return "houba hop";
	}

	@Override
	public DTOApplication getApplication(String name) {
		log.debug(String.format("getApplication service was called for app %s",
				name));
		Application a = DemoApplication.getNewDemoApplication();
		// TODO: really look for the application instead of test one

		DTOApplication d = Frontier.getApplication(a);
		log.debug("End of getApplication call. Returning an application.");
		return d;
	}

	/*
	 * @Override public DTOApplication getApplication(String name, Boolean
	 * byUuid) { // TODO Auto-generated method stub System.err.println("oups2");
	 * return null;// DemoApplication.getNewDemoApplication(); }
	 */

	@Override
	public DTOChain getChain() {
		log.debug("getChain service was called");
		Application a = DemoApplication.getNewDemoApplication();
		Chain c = a.getChains().get(0);
		DTOChain d = Frontier.getChain(c);
		log.debug("End of getChain call. Returning a chain.");
		return d;
	}

	@Override
	public void stageApplication(DTOApplication app) {
		// TODO Replace test code with true persistence and reboot engine
		// context
		log.debug("stageApplication service was called");

		Application a = DemoApplication.getNewDemoApplication();
		Chain c = a.getChains().get(0);

		System.out.println(app.chains.get(0).states.get(0).getX());
		System.out.println(c.getStates().get(0).getX());

		log.debug("End of stageApplication call.");
	}

	@Override
	public void storeApplication(String uuid) {
		// TODO Auto-generated method stub
		log.debug("storeApplication service was called");
		log.debug("End of storeApplication call.");
	}

	@Override
	public void resetStage() {
		// TODO Auto-generated method stub
		log.debug("resetStage service was called");
		log.debug("End of resetStage call.");
	}

}
