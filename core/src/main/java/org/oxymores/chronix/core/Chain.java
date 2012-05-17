/**
 * @author Marc-Antoine Gouillart
 * 
 * See the NOTICE file distributed with this work for 
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file 
 * except in compliance with the License. You may obtain 
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.oxymores.chronix.core;

import java.util.ArrayList;

public class Chain extends ActiveNodeBase {

	private static final long serialVersionUID = -5369294333404575011L;

	protected ArrayList<State> states;
	protected ArrayList<Transition> transitions;

	public Chain() {
		super();
		states = new ArrayList<State>();
		transitions = new ArrayList<Transition>();
	}

	public void addState(State state) {
		if (!this.states.contains(state)) {
			this.states.add(state);
			state.chain = this;
		}
	}

	public ArrayList<State> getStates() {
		return states;
	}

	public void addTransition(Transition tr) {
		if (!this.transitions.contains(tr)) {
			this.transitions.add(tr);
			tr.chain = this;
		}
	}

	public ArrayList<Transition> getTransitions() {
		return transitions;
	}
}
