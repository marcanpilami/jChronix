package org.oxymores.chronix.core.active;

import org.oxymores.chronix.core.ActiveNodeBase;

public class ChainEnd extends ActiveNodeBase {

	private static final long serialVersionUID = 4129809921422152571L;

	public ChainEnd() {
		this.name = "Chain end";
	}

	@Override
	public boolean visibleInHistory() {
		return false;
	}
}
