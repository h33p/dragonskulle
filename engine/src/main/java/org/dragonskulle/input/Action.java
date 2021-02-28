/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

/**
 * A possible action that could be activated.
 *
 * @author Craig Wilbourne
 */
public class Action {

	/**
	 * A name used for display purposes only.
	 */
	private String mName;
	
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
}
