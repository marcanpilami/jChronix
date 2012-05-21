package org.oxymores.chronix.engine;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.Event;

public class EventListener implements MessageListener {

	private static Logger log = Logger.getLogger(ChronixContext.class);

	boolean run = true;

	@Override
	public void onMessage(Message msg) {

		ObjectMessage omsg = (ObjectMessage) msg;
		Event evt;
		try {
			evt = (Event) omsg.getObject();
		} catch (JMSException e) {
			log.error(
					"An error occurred during event reception. BAD. Will go on",
					e);
			return;
		}

		log.debug(String.format("Event id %s received", evt.getId()));
	}
}
