package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.List;

import org.oxymores.chronix.core.transactional.Event;

public class EventAnalysisResult {
	public Boolean res = false;
	public List<Event> consumedEvents = new ArrayList<Event>();
	
	public void add(EventAnalysisResult ear)
	{
		res = res && ear.res;
		consumedEvents.addAll(ear.consumedEvents);
	}
}
