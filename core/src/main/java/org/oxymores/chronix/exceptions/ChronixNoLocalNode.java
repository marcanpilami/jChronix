package org.oxymores.chronix.exceptions;

public class ChronixNoLocalNode extends ChronixException {

	private static final long serialVersionUID = 1L;

	public ChronixNoLocalNode(String localElementDescription) {
		super(
				String.format(
						"Local node could not be found in metadata with request %s. Check your metadata.",
						localElementDescription));
	}
}
