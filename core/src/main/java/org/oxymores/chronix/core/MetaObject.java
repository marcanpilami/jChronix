package org.oxymores.chronix.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.UUID;

public class MetaObject implements Serializable
{
	private static final long serialVersionUID = 1106120751950998543L;

	public MetaObject()
	{
		UUID temp = UUID.randomUUID();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
			dos.writeLong(temp.getMostSignificantBits());
			dos.writeLong(temp.getLeastSignificantBits());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		id = new BigInteger(1, baos.toByteArray());
		
	}
	protected BigInteger id;

	public BigInteger getId() {
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

	public void setId(BigInteger id) {
		this.id = id;
	}
}
