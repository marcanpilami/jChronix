package org.oxymores.chronix.core.transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ChronixObject;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;

@Entity
public class TranscientBase implements Serializable {
	private static final long serialVersionUID = 8976655465578L;

	@Id
	@Column(columnDefinition="CHAR(36)")
	protected String id;
	@Column(columnDefinition="CHAR(36)")
	protected String stateID;
	@Column(columnDefinition="CHAR(36)")
	protected String placeID;
	@Column(columnDefinition="CHAR(36)")
	protected String appID;
	protected Date createdAt;

	protected ArrayList<EnvironmentValue> envParams;

	public TranscientBase() {
		id = UUID.randomUUID().toString();
		createdAt = new Date();
		envParams = new ArrayList<EnvironmentValue>();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChronixObject))
			return false;
		return ((ChronixObject) o).getId().equals(this.getId());
	}

	protected String getStateID() {
		return stateID;
	}

	protected void setStateID(String stateID) {
		this.stateID = stateID;
	}

	public State getState(ChronixContext ctx) {
		return this.getApplication(ctx).getState(UUID.fromString(this.stateID));
	}

	protected String getPlaceID() {
		return placeID;
	}

	protected void setPlaceID(String placeID) {
		this.placeID = placeID;
	}

	public Place getPlace(ChronixContext ctx) {
		return this.getApplication(ctx).getPlace(UUID.fromString(this.placeID));
	}

	protected String getAppID() {
		return appID;
	}

	protected void setAppID(String appID) {
		this.appID = appID;
	}

	public Application getApplication(ChronixContext ctx) {
		return ctx.applicationsById.get(UUID.fromString(this.appID));
	}

	public void setApplication(Application application) {
		if (application != null)
			this.appID = application.getId().toString();
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

	public String getId() {
		return id;
	}

	@SuppressWarnings("unused")
	private void setId(String id) {
		this.id = id;
	}

	public ArrayList<EnvironmentValue> getEnvParams() {
		return envParams;
	}

	protected void setEnvParams(ArrayList<EnvironmentValue> values) {
		this.envParams = values;
	}

	public void addValue(String key, String value) {
		this.envParams.add(new EnvironmentValue(key, value));
	}
}