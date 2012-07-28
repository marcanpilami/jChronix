package org.oxymores.chronix.core.active;

import org.oxymores.chronix.core.ActiveNodeBase;

public class Or extends ActiveNodeBase {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean visibleInHistory() {
		return false;
	}
}
