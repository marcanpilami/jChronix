package org.oxymores.chronix.web;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

@Component
public class Activator implements BundleActivator
{
    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) throws Exception
    {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("osgi.http.whiteboard.servlet.pattern", "/ws/*");
        props.put("servlet.init.javax.ws.rs.Application", RestApp.class.getName());

        this.registration = context.registerService(Servlet.class.getName(), new ServletContainer(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (this.registration != null)
        {
            this.registration.unregister();
        }
    }

}
