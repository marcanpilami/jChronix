package org.oxymores.chronix.web.core;

import java.util.HashMap;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.moxy.json.internal.ConfigurableMoxyJsonProvider;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApplicationConfiguration extends ResourceConfig
{
    public RestApplicationConfiguration()
    {
        // Use Moxy with a few wrapping properties
        this.register(new MoxyXmlFeature(new HashMap<String, Object>(), this.getClass().getClassLoader(), false));
        this.register(new ConfigurableMoxyJsonProvider());

        this.property(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
        this.property(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

        // Don't use auto discovery at all.
        this.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);

        // Add the error listener
        this.register(ErrorListener.class);
    }

}
