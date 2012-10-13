package org.oxymores.chronix.engine;

import java.io.Serializable;

public class Order implements Serializable {
	private static final long serialVersionUID = 6731249888476299895L;

	public OrderType type;
	public Object data, data2;
}
