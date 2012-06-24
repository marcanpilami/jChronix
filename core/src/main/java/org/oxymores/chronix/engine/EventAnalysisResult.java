package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.transactional.Event;

public class EventAnalysisResult {
	public Boolean res = false;
	public List<Event> consumedEvents = new ArrayList<Event>();
	//public Hashtable<Place, Boolean> ress = new Hashtable<Place, Boolean>();
	
	public void addParallel(EventAnalysisResult ear)
	{
		
	}
	
	public void add(EventAnalysisResult ear)
	{
		res = res && ear.res;
		consumedEvents.addAll(ear.consumedEvents);
	}
}
