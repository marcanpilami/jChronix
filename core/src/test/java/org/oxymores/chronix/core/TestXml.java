package org.oxymores.chronix.core;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.oxymores.chronix.engine.TestChain;

@SuppressWarnings("unused")
public class TestXml {
	private static Logger log = Logger.getLogger(TestXml.class);

	private String db1;
	Application a1;
	
	
	@Test
	public void testXml() throws Exception
	{
		Application a1 = org.oxymores.chronix.demo.DemoApplication
				.getNewDemoApplication();

		JAXBContext ctx = JAXBContext.newInstance(Application.class);
		Marshaller m = ctx.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		m.marshal(a1, System.out);
	}
}
