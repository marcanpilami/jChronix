package org.oxymores.chronix.core.timedata;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class RunMetrics implements Serializable {
	private static final long serialVersionUID = -4424619647312566179L;

	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String stateId;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String placeId;
	public Long duration;
	public Date startTime;
}
