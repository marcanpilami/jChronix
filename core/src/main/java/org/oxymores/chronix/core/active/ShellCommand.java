package org.oxymores.chronix.core.active;

import org.oxymores.chronix.core.ActiveNodeBase;

public class ShellCommand extends ActiveNodeBase{

	private static final long serialVersionUID = 3340501935290198518L;

	protected String command;

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
}
