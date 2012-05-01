package org.oxymores.chronix.event;
import java.util.Date;
import java.util.UUID;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.MetaObject;

public class TranscientBase extends MetaObject 
{
	private static final long serialVersionUID = 8976655465578L;

	protected UUID stateID;
	protected UUID placeID;
	
	protected UUID appID;
	protected transient Application application;
	
	public UUID getStateID() {
		return stateID;
	}

	public void setStateID(UUID stateID) {
		this.stateID = stateID;
	}

	public UUID getPlaceID() {
		return placeID;
	}

	public void setPlaceID(UUID placeID) {
		this.placeID = placeID;
	}

	public UUID getAppID() {
		return appID;
	}

	public void setAppID(UUID appID) {
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
