package org.oxymores.chronix.core.transactional;

import java.util.UUID;

import org.oxymores.chronix.core.ChronixObject;

public class EnvironmentValue {

	private UUID id;
	private String key, value;

	TranscientBase associatedTo;

	public EnvironmentValue() {
		id = UUID.randomUUID();
	}

	public EnvironmentValue(String key, String value) {
		id = UUID.randomUUID();
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChronixObject))
			return false;
		return ((ChronixObject) o).getId().equals(this.getId());
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public TranscientBase getAssociatedTo() {
		return associatedTo;
	}

	public void setAssociatedTo(TranscientBase associatedTo) {
		this.associatedTo = associatedTo;
	}
}
