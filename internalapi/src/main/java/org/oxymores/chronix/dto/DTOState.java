package org.oxymores.chronix.dto;

import java.math.BigInteger;

public class DTOState {
	protected BigInteger id;
	protected Integer x, y;
	protected String label;
	protected Integer WarnAfterMn, KillAfterMn, MaxPipeWaitTime, EventValidityMn;
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Integer getWarnAfterMn() {
		return WarnAfterMn;
	}

	public void setWarnAfterMn(Integer warnAfterMn) {
		WarnAfterMn = warnAfterMn;
	}

	public Integer getKillAfterMn() {
		return KillAfterMn;
	}

	public void setKillAfterMn(Integer killAfterMn) {
		KillAfterMn = killAfterMn;
	}

	public Integer getMaxPipeWaitTime() {
		return MaxPipeWaitTime;
	}

	public void setMaxPipeWaitTime(Integer maxPipeWaitTime) {
		MaxPipeWaitTime = maxPipeWaitTime;
	}

	public Integer getEventValidityMn() {
		return EventValidityMn;
	}

	public void setEventValidityMn(Integer eventValidityMn) {
		EventValidityMn = eventValidityMn;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public BigInteger getId() {
		return id;
	}

	public void setId(BigInteger id) {
		this.id = id;
	}
}
