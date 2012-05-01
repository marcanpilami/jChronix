package org.oxymores.chronix.dto;

import java.util.ArrayList;

import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Transition;

public class Frontier {

	public static DTOChain getChain(Chain c)
	{
		DTOChain res = new DTOChain();
		res.id = c.getId().toString();
		res.name = c.getName();
		res.description = c.getDescription();
		res.states = new ArrayList<DTOState>();
		res.transitions = new ArrayList<DTOTransition>();
		
		for (State s : c.getStates())
		{
			DTOState t = new DTOState();
			t.id = s.getId().toString();
			t.x = s.getX();
			t.y = s.getY();
			res.states.add(t);
		}
		
		for (Transition o : c.getTransitions())
		{
			DTOTransition d = new DTOTransition();
			d.id = o.getId().toString();
			d.from = o.getStateFrom().getId().toString();
			d.to = o.getStateTo().getId().toString();
			d.guard1 = o.getGuard1();
			d.guard2 = o.getGuard2();
			d.guard3 = o.getGuard3();
			d.guard4 = (o.getGuard4() == null ? "": o.getGuard4().toString());
			
			res.transitions.add(d);
		}
		
		return res;
	}
}
