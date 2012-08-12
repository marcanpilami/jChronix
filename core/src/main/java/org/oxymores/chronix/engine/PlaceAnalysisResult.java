package org.oxymores.chronix.engine;

import java.util.ArrayList;

import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.transactional.Event;

public class PlaceAnalysisResult {
	public PlaceAnalysisResult(Place p) {
		this.place = p;
	}

	public boolean res = false;
	public Place place;
	public ArrayList<Event> consumedEvents = new ArrayList<Event>();
	public ArrayList<Event> usedEvents = new ArrayList<Event>();

	public void add(PlaceAnalysisResult ear) {
		res = res && ear.res;
		if (res)
			consumedEvents.addAll(ear.consumedEvents);
		else
			consumedEvents.clear();

		if (res)
			usedEvents.addAll(ear.usedEvents);
		else
			usedEvents.clear();
	}
}
