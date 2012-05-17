package org.oxymores.chronix.core;

import java.io.Serializable;
import java.util.UUID;

public class ChronixObject implements Serializable {
	private static final long serialVersionUID = 1106120751950998543L;

	public ChronixObject() {
		id = UUID.randomUUID();

	}

	protected UUID id;

	public UUID getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChronixObject))
			return false;
		return ((ChronixObject) o).getId().equals(this.getId());
	}

	public boolean validate() {
		return false;
	}

	public void setId(UUID id) {
		this.id = id;
	}
}
