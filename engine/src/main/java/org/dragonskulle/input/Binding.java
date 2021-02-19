package org.dragonskulle.input;

import java.util.ArrayList;

class Binding {

	/** The button code. */
	private int mButton;
	/** The actions the button activates. */
	private ArrayList<Action> mActions = new ArrayList<Action>();
	
	/**
	 * Create a binding for a button that trigger no actions.
	 * 
	 * @param button The button.
	 */
	public Binding(int button) {
		setButton(button);
	}
	
	/**
	 * Create a binding for a button that triggers a set of actions.
	 * 
	 * @param button The button.
	 * @param actions The actions the button triggers, provided as an {@code ArrayList}.
	 */
	public Binding(int button, ArrayList<Action> actions) {
		setButton(button);
		addActions(actions);
	}
	
	/**
	 * Create a binding for a button that triggers a set of actions.
	 * 
	 * @param button The button.
	 * @param actions The actions the button triggers, provided as a series of arguments.
	 */
	public Binding(int button, Action... actions) {
		setButton(button);
		addActions(actions);
	}
	
	/**
	 * Set the button code.
	 * 
	 * @param button The code, as an {@code int}.
	 */
	public void setButton(int button) {
		mButton = button;
	}
	
	/**
	 * Add a single action.
	 * 
	 * @param action The action.
	 */
	public void addAction(Action action) {
		if(action != null) {
			mActions.add(action);
		}		
	}
	
	/**
	 * Add several actions via an {@code ArrayList}.
	 * 
	 * @param actions The {@code ArrayList} of actions.
	 */
	public void addActions(ArrayList<Action> actions) {
		if(actions != null) {
			mActions.addAll(actions);
		}		
	}
	
	/**
	 * Add several actions via multiple arguments.
	 * 
	 * @param actions The actions passed in as arguments.
	 */
	public void addActions(Action... actions) {
		for (Action action : actions) {
			addAction(action);
		}		
	}
	
	/**
	 * Get the button code.
	 * 
	 * @return The button code
	 */
	int getButton(){
		return mButton;
	}
	
	/**
	 * Get the actions the button triggers.
	 * 
	 * @return The actions.
	 */
	ArrayList<Action> getActions(){
		return mActions;
	}
	
}
