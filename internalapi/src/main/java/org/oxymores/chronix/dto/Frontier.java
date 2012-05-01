package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.UUID;

import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.State;

public class Frontier {

	public static DTOChain getChain(Chain c)
	{
		DTOChain res = new DTOChain();
		res.id = c.getId().toString();
		res.name = c.getName();
		res.description = c.getDescription();
		res.states = new ArrayList<DTOState>();
		res.truc = UUID.randomUUID();
		
		for (State s : c.getStates())
		{
			DTOState t = new DTOState();
			t.id = s.getId().toString();
			t.x = s.getX();
			t.y = s.getY();
			res.states.add(t);
		}
		
		return res;
	}
}
