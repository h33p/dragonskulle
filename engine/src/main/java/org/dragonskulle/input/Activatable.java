package org.dragonskulle.input;

import java.util.HashMap;

abstract class Activatable<T> {

	private final HashMap<T, Boolean> stored = new HashMap<T, Boolean>();
	
	void setActivated(T key, Boolean value){
		stored.put(key, value);
	}
	
	public boolean isActivated(T key) {
		if(key == null || !stored.containsKey(key)) {
			return false;
		}
		Boolean value = stored.get(key);
		if(value == null) {
			return false;
		}
		return value.booleanValue();
	}	
	
	boolean contains(T key) {
		if(key == null || !stored.containsKey(key)) {
			return false;
		}
		return true;
	}
	
}
