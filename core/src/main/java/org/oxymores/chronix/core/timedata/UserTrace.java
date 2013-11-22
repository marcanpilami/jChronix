package org.oxymores.chronix.core.timedata;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.oxymores.chronix.engine.helpers.OrderType;

@Entity
public class UserTrace
{
	@Column(length = 36)
	@Id
	public String id; // UUID
	public OrderType type;
	@Column(length = 40)
	public String username;
	@Column(length = 100)
	public String details;
	public Date submitted, ended;
}
