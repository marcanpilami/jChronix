package org.oxymores.chronix.event;
import java.math.BigInteger;
import java.util.Date;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.MetaObject;

public class TranscientBase extends MetaObject 
{
	private static final long serialVersionUID = 8976655465578L;

	protected BigInteger stateID;
	protected BigInteger placeID;
	
	protected BigInteger appID;
	protected transient Application application;
	
	public BigInteger getStateID() {
		return stateID;
	}

	public void setStateID(BigInteger stateID) {
		this.stateID = stateID;
	}

	public BigInteger getPlaceID() {
		return placeID;
	}

	public void setPlaceID(BigInteger placeID) {
		this.placeID = placeID;
	}

	public BigInteger getAppID() {
		return appID;
	}

	public void setAppID(BigInteger appID) {
		this.appID = appID;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
		if (application != null)
			this.appID = application.getId();
		else
			this.appID = null;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getEnqueued() {
		return enqueued;
	}

	public void setEnqueued(Date enqueued) {
		this.enqueued = enqueued;
	}

	public Date getQueueSlotEnd() {
		return queueSlotEnd;
	}

	public void setQueueSlotEnd(Date queueSlotEnd) {
		this.queueSlotEnd = queueSlotEnd;
	}

	public Date getRunSlotEnd() {
		return runSlotEnd;
	}

	public void setRunSlotEnd(Date runSlotEnd) {
		this.runSlotEnd = runSlotEnd;
	}

	protected Date created, enqueued, queueSlotEnd, runSlotEnd;
	
	
}
