package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.glfw.GLFW;

/**
 * Converts between which buttons and {@link Action}s.
 * 
 * @author Craig Wilbourne
 */
public class Bindings {
	
	/** Key: Button <br/>
	 *  Value: {@link Action}s the Button activates.
	 * */
	private final HashMap<Integer, ArrayList<Action>> mButtonToActions = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	private final HashMap<Action, ArrayList<Integer>> mActionToButtons = new HashMap<Action, ArrayList<Integer>>();
	
	public Bindings() {
		// Currently hard-coded values:
		
		mButtonToActions.put(Scroll.UP, getActionList(Action.SCROLL_UP, Action.ZOOM_IN));
		mButtonToActions.put(Scroll.DOWN, getActionList(Action.SCROLL_DOWN, Action.ZOOM_OUT));
		
		mButtonToActions.put(GLFW.GLFW_KEY_UP, getActionList(Action.UP, Action.SCROLL_UP));
		mButtonToActions.put(GLFW.GLFW_KEY_W, getActionList(Action.UP, Action.SCROLL_UP));
		mButtonToActions.put(GLFW.GLFW_KEY_DOWN, getActionList(Action.DOWN, Action.SCROLL_DOWN));
		mButtonToActions.put(GLFW.GLFW_KEY_S, getActionList(Action.DOWN, Action.SCROLL_DOWN));
		
		mButtonToActions.put(GLFW.GLFW_KEY_LEFT, getActionList(Action.LEFT));
		mButtonToActions.put(GLFW.GLFW_KEY_A, getActionList(Action.LEFT));
		mButtonToActions.put(GLFW.GLFW_KEY_RIGHT, getActionList(Action.RIGHT));
		mButtonToActions.put(GLFW.GLFW_KEY_D, getActionList(Action.RIGHT));
		
		mButtonToActions.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, getActionList(Action.ACTION_1, Action.DRAG));
		mButtonToActions.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, getActionList(Action.ACTION_2));
		mButtonToActions.put(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, getActionList(Action.ACTION_3));
		
		generateActionToButton();
		
		System.out.println(mButtonToActions);
		System.out.println(mActionToButtons);
	}
	
	/**
	 * Use {@link #buttonToAction} to generate the contents of {@link #actionToButton}.
	 */
	private void generateActionToButton() {
		for (Entry<Integer, ArrayList<Action>> entry : mButtonToActions.entrySet()) {
			for (Action action : entry.getValue()) {
				ArrayList<Integer> buttonsList = new ArrayList<Integer>();
				
				buttonsList.add(entry.getKey());
				if(mActionToButtons.containsKey(action)) {
					buttonsList.addAll(mActionToButtons.get(action));
				}
				
				mActionToButtons.put(action, buttonsList);
			}
		}
	}
	
	// TODO: REMOVE
	private ArrayList<Action> getActionList(Action... selectedActions){
		ArrayList<Action> list = new ArrayList<Action>();
		
		for (Action action : selectedActions) {
			list.add(action);
		}
		
		return list;
	}
	
	/**
	 * @param button The button being targeted.
	 * 
	 * @return An {@code ArrayList} of {@link Action}s associated with the button, or an empty {@code ArrayList}.
	 */
	public ArrayList<Action> getActions(Integer button) {
		if(!mButtonToActions.containsKey(button)) {
			return new ArrayList<Action>();
		}
		return mButtonToActions.get(button);
	}
	
	/***
	 * @param action The action being targeted.
	 * 
	 * @return An {@code ArrayList} of buttons associated with the {@link Action}, or an empty {@code ArrayList}.
	 */
	public ArrayList<Integer> getButtons(Action action) {
		if(!mActionToButtons.containsKey(action)) {
			return new ArrayList<Integer>();
		}
		return mActionToButtons.get(action);
	}
	
}
