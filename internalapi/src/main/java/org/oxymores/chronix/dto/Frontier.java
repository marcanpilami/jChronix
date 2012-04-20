package org.oxymores.chronix.dto;

import java.util.ArrayList;

import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.State;

public class Frontier {

	public static DTOChain getChain(Chain c)
	{
		DTOChain res = new DTOChain();
		res.id = c.getId();
		res.name = c.getName();
		res.description = c.getDescription();
		res.states = new ArrayList<DTOState>();
		
		for (State s : c.getStates())
		{
			DTOState t = new DTOState();
			t.id = s.getId();
			res.states.add(t);
		}
		
		return res;
	}
}
