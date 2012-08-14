package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.Transition;
import org.oxymores.chronix.core.transactional.Event;

public class TransitionAnalysisResult {
	public Transition transition;
	public boolean parallel;
	public HashMap<UUID, PlaceAnalysisResult> analysis = new HashMap<UUID, PlaceAnalysisResult>();

	public TransitionAnalysisResult(Transition tr) {
		this.transition = tr;
		this.parallel = tr.isTransitionParallelEnabled();
	}

	public ArrayList<Event> getConsumedEvents() {
		ArrayList<Event> res = new ArrayList<Event>();
		for (PlaceAnalysisResult par : this.analysis.values()) {
			if (!par.res)
				continue;
			for (Event e : par.consumedEvents) {
				if (!res.contains(e))
					res.add(e);
			}
		}
		return res;
	}

	public ArrayList<Event> getUsedEvents() {
		ArrayList<Event> res = new ArrayList<Event>();
		for (PlaceAnalysisResult par : this.analysis.values()) {
			if (!par.res)
				continue;
			for (Event e : par.usedEvents) {
				if (!res.contains(e))
					res.add(e);
			}
		}
		return res;
	}

	public boolean allowedOnAllPlaces() {
		boolean res = true;
		for (PlaceAnalysisResult par : this.analysis.values())
			res = res && par.res;
		return res;
	}

	public boolean totallyBlocking() {
		for (PlaceAnalysisResult par : this.analysis.values())
			if (par.res)
				return false;
		return true;
	}

	public boolean allowedOnPlace(Place p) {
		if ((!this.parallel && this.allowedOnAllPlaces()) || (this.analysis.get(p.getId()).res && this.parallel))
			return true;
		return false;
	}

	public ArrayList<Event> eventsConsumedOnPlace(Place p) {
		if (!this.parallel && this.allowedOnAllPlaces()) {
			return this.getConsumedEvents();
		}
		if (this.parallel && this.analysis.get(p.getId()).res)
			return this.analysis.get(p.getId()).consumedEvents;

		return new ArrayList<Event>();
	}
}