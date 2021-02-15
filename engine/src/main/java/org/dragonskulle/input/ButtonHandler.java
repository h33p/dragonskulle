package org.dragonskulle.input;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

class ButtonHandler {
	
	/** Stores whether keyboard and mouse buttons are pressed or released.*/
	private final HashMap<Integer, Boolean> buttons = new HashMap<Integer, Boolean>();

	void setActivated(Integer key, Boolean value){
		buttons.put(key, value);
	}
	
	boolean isActivated(Integer key){
		if(key == null || !buttons.containsKey(key)) {
			return false;
		}
		Boolean value = buttons.get(key);
		if(value == null) {
			return false;
		}
		return value.booleanValue();
	}
	
	boolean contains(Integer key) {
		if(key == null || !buttons.containsKey(key)) {
			return false;
		}
		return true;
	}
	
}
