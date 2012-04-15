package org.oxymores.chronix.core;

import java.util.ArrayList;

public class Chain extends ActiveNodeBase {

	private static final long serialVersionUID = -5369294333404575011L;

	protected ArrayList<State> states;
	
	public Chain(){
		super();
		states = new ArrayList<State>();
	}
	
	public void addState(State state)
	{
		if (!this.states.contains(state))
		{
			this.states.add(state);
			state.chain = this;
		}
	}
}
