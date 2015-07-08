package org.oxymores.chronix.core;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class TestXml
{
    private static Logger log = Logger.getLogger(TestXml.class);

    @Test
    public void testXmlXstream()
    {
        XStream x = new XStream(new StaxDriver());
        x.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);

        Application a1 = org.oxymores.chronix.planbuilder.DemoApplication.getNewDemoApplication();
        x.marshal(a1, new PrettyPrintWriter(new OutputStreamWriter(new BufferedOutputStream(System.out))));
    }
}
