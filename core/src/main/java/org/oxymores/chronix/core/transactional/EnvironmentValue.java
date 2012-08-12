package org.oxymores.chronix.core.transactional;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.oxymores.chronix.core.ChronixObject;

@Entity
public class EnvironmentValue implements Serializable {

	private static final long serialVersionUID = -3301527648471127170L;

	@Id
	@Column(nullable = false, length = 36)
	private String id;
	@Column(length = 50)
	private String key, value;

	// @ManyToOne(cascade=CascadeType.ALL, )
	// TranscientBase associatedTo;

	public EnvironmentValue() {
		id = UUID.randomUUID().toString();
	}

	public EnvironmentValue(String key, String value) {
		id = UUID.randomUUID().toString();
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChronixObject))
			return false;
		return ((ChronixObject) o).getId().equals(this.getId());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
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
	/*
	 * public TranscientBase getAssociatedTo() { return associatedTo; }
	 * 
	 * public void setAssociatedTo(TranscientBase associatedTo) {
	 * this.associatedTo = associatedTo; }
	 */
}
