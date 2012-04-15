package org.oxymores.chronix.core;

import java.io.Serializable;
import java.util.UUID;

public class MetaObject implements Serializable
{
	private static final long serialVersionUID = 1106120751950998543L;

	public MetaObject()
	{
		id = UUID.randomUUID();
	}
	protected UUID id;

	public UUID getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (! (o instanceof MetaObject))
			return false;
		return ((MetaObject)o).getId().equals(this.getId());
	}
	
	public boolean validate()
	{
		return false;
	}
}
