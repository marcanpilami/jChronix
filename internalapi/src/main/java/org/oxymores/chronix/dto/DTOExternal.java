package org.oxymores.chronix.dto;

public class DTOExternal
{
	public String id, name, description;
	public String machineRestriction, accountRestriction;
	public String regularExpression;

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getMachineRestriction()
	{
		return machineRestriction;
	}

	public void setMachineRestriction(String machineRestriction)
	{
		this.machineRestriction = machineRestriction;
	}

	public String getAccountRestriction()
	{
		return accountRestriction;
	}

	public void setAccountRestriction(String accountRestriction)
	{
		this.accountRestriction = accountRestriction;
	}

	public String getRegularExpression()
	{
		return regularExpression;
	}

	public void setRegularExpression(String regularExpression)
	{
		this.regularExpression = regularExpression;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
