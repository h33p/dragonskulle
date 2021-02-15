package org.dragonskulle.input;

import java.util.HashMap;

class ActionHandler {
	
	/** Stores whether a specific {@link Action} is being activated. */
	private final HashMap<Action, Boolean> actions = new HashMap<Action, Boolean>();
	
	void setActivated(Action action, Boolean value){
		actions.put(action, value);
	}
	
	public boolean isActivated(Action action) {
		if(action == null || !actions.containsKey(action)) {
			return false;
		}
		Boolean value = actions.get(action);
		if(value == null) {
			return false;
		}
		return value.booleanValue();
	}	
	
}
