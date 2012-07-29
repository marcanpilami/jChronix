package org.oxymores.chronix.core.transactional;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

import org.apache.openjpa.persistence.InverseLogical;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public class EventConsumption extends TranscientBase {

	private static final long serialVersionUID = 4960077419503476652L;

	@ManyToOne(cascade = CascadeType.ALL)
	@InverseLogical("consumptions")
	public Event event;

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}
}
