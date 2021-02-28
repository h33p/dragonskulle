/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

/**
 * An action that stores whether it is activated or not.
 * <p>
 * An action is triggered by buttons, as defined in {@link CustomBindings}. 
 *
 * @author Craig Wilbourne
 */
public class Action {

	/** A name used for display purposes only. */
	private String mName;
	
	/**	Whether the action is currently activated. */
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
	
	/**
	 * Get if the action is currently activated.
	 * 
	 * @return Whether the action is currently activated.
	 */
	public boolean isActivated() {
		return mActivated;
	}

	/**
	 * Set the action to either activated ({@code true}) or not activated ({@code false}).
	 * 
	 * @param activated Whether the action should be activated.
	 */
	void setActivated(boolean activated) {
		mActivated = activated;
	}
	
	@Override
	public String toString() {
		if(mName == null) {
			// If no name is available, display the action name as blank.
			return String.format("Action{name:---}", mName);
		}
		return String.format("Action{name:%s}", mName);
	}
}
