/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import lombok.Getter;

/**
 * An action.
 * <p>
 * Using {@link CustomBindings}, this action can be triggered by any number of button presses. 
 *
 * @author Craig Wilbourne
 */
public class Action {

	/**
	 * A name used for display purposes only.
	 */
	private String mName;
	
	private Boolean mActivated = false;
	
	/**
	 * Create a new (unnamed) action.
	 */
	public Action() {}
	
	/**
	 * Create an action and give it a display name.
	 * 
	 * @param name The name of the action.
	 */
	public Action(String name) {
		this();
		this.mName = name;
	}
	
	@Override
	public String toString() {
		if(mName == null) {
			// If no name is available, display the action name as blank.
			return String.format("Action{name:---}", mName);
		}
		return String.format("Action{name:%s}", mName);
	}

	public boolean isActivated() {
		return mActivated;
	}

	public void setActivated(boolean activated) {
		mActivated = activated;
	}
}
