package org.oxymores.chronix.core.transactional;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;

@Entity
public class ClockTick implements Serializable {
	private static final long serialVersionUID = 4194251899101238989L;
	
	public String ClockId;
	public Date TickTime;
}
