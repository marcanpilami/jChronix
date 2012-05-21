package org.oxymores.chronix.engine;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;

public class MetadataListener implements MessageListener {

	private static Logger log = Logger.getLogger(MetadataListener.class);
	private Session session;
	private ChronixContext ctx;

	public MetadataListener(Session s, ChronixContext c) {
		log.debug(String.format("Initializing MetadataListener on context %s", c.configurationDirectory));
		this.session = s;
		this.ctx = c;
	}

	@SuppressWarnings("finally")
	@Override
	public void onMessage(Message msg) {
		log.debug(String.format("An application was received (local node db is %s)", ctx.configurationDirectory));
		ObjectMessage omsg = (ObjectMessage) msg;
		Application a;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof Application)) {
				log.warn("An object was received on the app queue but was not an app! Ignored.");
				return;
			}

			a = (Application) o;
		} catch (JMSException e) {
			log.error(
					"An error occurred during metadata message reception. BAD. Will go on",
					e);
			return;
		}

		try {
			log.debug("Saving received app as the current working copy");
			ctx.saveApplication(a);
		} catch (Exception e1) {
			log.error(
					"Issue while trying to commit to disk an application received from another node. The application sent will be thrown out.",
					e1);
			try {
				session.commit(); // if read again, would still fail.
			} finally {
				return;
			}
		}

		try {
			log.debug("Setting the new app version as the active version");
			ctx.setWorkingAsCurrent(a);
		} catch (Exception e1) {
			log.error(
					"An application was correctly received and saved to disk. However, it could not be activated, which requires a file to be copied. Check log and try again sending the application (or activate it manually.)",
					e1);
		}

		// TODO: recycle engine.

		try {
			session.commit();
		} catch (JMSException e) {
			log.error(
					"While receiving an application definition, we could not commit reading the message. It is a catastrophy: the db has already been correctly commited with the application data. The scheduler will stop. Empty the APPLICATION queue and restart it.",
					e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
		log.debug(String.format("Application of id %s received", a.getId()));
	}
}
