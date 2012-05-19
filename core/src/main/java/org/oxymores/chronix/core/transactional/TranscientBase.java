package org.oxymores.chronix.core.transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;

public class TranscientBase implements Serializable {
	private static final long serialVersionUID = 8976655465578L;

	protected UUID id;
	protected UUID stateID;
	protected UUID placeID;
	protected UUID appID;
	protected Date createdAt;

	protected ArrayList<EnvironmentValue> values;

	public TranscientBase() {
		id = UUID.randomUUID();
		createdAt = new Date();
		values = new ArrayList<EnvironmentValue>();
	}

	protected UUID getStateID() {
		return stateID;
	}

	protected void setStateID(UUID stateID) {
		this.stateID = stateID;
	}

	public State getState(ChronixContext ctx) {
		return this.getApplication(ctx).getState(this.stateID);
	}

	protected UUID getPlaceID() {
		return placeID;
	}

	protected void setPlaceID(UUID placeID) {
		this.placeID = placeID;
	}

	public Place getPlace(ChronixContext ctx) {
		return this.getApplication(ctx).getPlace(this.placeID);
	}

	protected UUID getAppID() {
		return appID;
	}

	protected void setAppID(UUID appID) {
		this.appID = appID;
	}

	public Application getApplication(ChronixContext ctx) {
		return ctx.applicationsById.get(this.appID);
	}

	public void setApplication(Application application) {
		if (application != null)
			this.appID = application.getId();
		else
			this.appID = null;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	@SuppressWarnings("unused")
	private void setCreatedAt(Date created) {
		this.createdAt = created;
	}

	public UUID getId() {
		return id;
	}

	@SuppressWarnings("unused")
	private void setId(UUID id) {
		this.id = id;
	}

	public ArrayList<EnvironmentValue> getValues() {
		return values;
	}

	protected void setValues(ArrayList<EnvironmentValue> values) {
		this.values = values;
	}

	public void addValue(String key, String value) {
		this.values.add(new EnvironmentValue(key, value));
	}
}
