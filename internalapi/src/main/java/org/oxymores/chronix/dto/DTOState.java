package org.oxymores.chronix.dto;

public class DTOState
{
	protected String id, representsId, runsOnId;
	protected Integer x, y;
	protected String label, runsOnName;
	protected Integer WarnAfterMn, KillAfterMn, MaxPipeWaitTime, EventValidityMn;
	protected boolean canReceiveLink = true, canEmitLinks = true;

	public String getRunsOnName()
	{
		return runsOnName;
	}

	public void setRunsOnName(String runsOnName)
	{
		this.runsOnName = runsOnName;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public Integer getWarnAfterMn()
	{
		return WarnAfterMn;
	}

	public void setWarnAfterMn(Integer warnAfterMn)
	{
		WarnAfterMn = warnAfterMn;
	}

	public Integer getKillAfterMn()
	{
		return KillAfterMn;
	}

	public void setKillAfterMn(Integer killAfterMn)
	{
		KillAfterMn = killAfterMn;
	}

	public Integer getMaxPipeWaitTime()
	{
		return MaxPipeWaitTime;
	}

	public void setMaxPipeWaitTime(Integer maxPipeWaitTime)
	{
		MaxPipeWaitTime = maxPipeWaitTime;
	}

	public Integer getEventValidityMn()
	{
		return EventValidityMn;
	}

	public void setEventValidityMn(Integer eventValidityMn)
	{
		EventValidityMn = eventValidityMn;
	}

	public Integer getX()
	{
		return x;
	}

	public void setX(Integer x)
	{
		this.x = x;
	}

	public Integer getY()
	{
		return y;
	}

	public void setY(Integer y)
	{
		this.y = y;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getRepresentsId()
	{
		return representsId;
	}

	public void setRepresentsId(String representsId)
	{
		this.representsId = representsId;
	}

	public String getRunsOnId()
	{
		return runsOnId;
	}

	public void setRunsOnId(String runsOnId)
	{
		this.runsOnId = runsOnId;
	}

	public boolean isCanReceiveLink()
	{
		return canReceiveLink;
	}

	public void setCanReceiveLink(boolean canReceiveLink)
	{
		this.canReceiveLink = canReceiveLink;
	}

	public boolean isCanEmitLinks()
	{
		return canEmitLinks;
	}

	public void setCanEmitLinks(boolean canEmitLinks)
	{
		this.canEmitLinks = canEmitLinks;
	}
}
