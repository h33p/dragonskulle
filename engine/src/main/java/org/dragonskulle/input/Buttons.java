package org.dragonskulle.input;

import java.util.ArrayList;

/**
 * Stores GLFW keyboard keys (as {@link Integer}s) and whether they are activated.
 * 
 * @author Craig Wilbourne
 */
class Buttons extends Activatable<Integer>{
	
	private Converter converter;
	private Actions actions;
	
	public Buttons(Converter converter, Actions actions) {
		this.converter = converter;
		this.actions = actions;
	}
	
	/** Called when a button has been pressed. */
	public void pressed(Integer button) {
		setActivated(button, true);
		
		for (Action action : converter.getActions(button)) {
			actions.setActivated(action, true);
		}
	}
	
	/** Called when a button has been released. */
	public void released(Integer button) {
		setActivated(button, false);
		
		// Check each action the button triggers, deactivating each action if no other buttons are currently triggering it.
		for (Action action : converter.getActions(button)) {			
			Boolean deactivate = true;
			
			for(Integer otherButton : converter.getButtons(action)) {
				// If another button that triggers the action is currently activated, do not set action to false.
				if(isActivated(otherButton) == true) {
					deactivate = false;
					break;
				}
			}
			
			// If no other buttons are triggering the action, deactivate the action.
			if(deactivate) {
				actions.setActivated(action, false);
			}
		}
	}
	
}
