package org.oxymores.chronix.wapi;

import org.slf4j.Logger;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.LoggerFactory;

/**
 Sometimes Jersey/Moxy errors are not sent to the log... This listener exists
 to trapp them anyway. It is disabled in production builds.
 */
public class ErrorListener implements ApplicationEventListener
{
    private static final Logger log = LoggerFactory.getLogger(ErrorListener.class);

    @Override
    public void onEvent(ApplicationEvent event)
    {

    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent)
    {
        return new ExceptionRequestEventListener();
    }

    public static class ExceptionRequestEventListener implements RequestEventListener
    {
        public ExceptionRequestEventListener()
        {
        }

        @Override
        public void onEvent(RequestEvent event)
        {
            switch (event.getType())
            {
                case ON_EXCEPTION:
                    Throwable t = event.getException();
                    log.error("houba beurk", t);
            }
        }
    }
}
