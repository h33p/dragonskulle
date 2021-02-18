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
	private final HashMap<Integer, ArrayList<Action>> buttonToActions = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	private final HashMap<Action, ArrayList<Integer>> actionToButtons = new HashMap<Action, ArrayList<Integer>>();
	
	public Bindings() {
		// Currently hard-coded values:
		
		//buttonToAction.put(Scroll.UP, getActionList(Action.SCROLL_UP, Action.ZOOM_IN));
		//buttonToAction.put(Scroll.DOWN, getActionList(Action.SCROLL_DOWN, Action.ZOOM_OUT));
		
		buttonToActions.put(GLFW.GLFW_KEY_UP, getActionList(Action.UP, Action.SCROLL_UP));
		buttonToActions.put(GLFW.GLFW_KEY_W, getActionList(Action.UP, Action.SCROLL_UP));
		buttonToActions.put(GLFW.GLFW_KEY_DOWN, getActionList(Action.DOWN, Action.SCROLL_DOWN));
		buttonToActions.put(GLFW.GLFW_KEY_S, getActionList(Action.DOWN, Action.SCROLL_DOWN));
		
		buttonToActions.put(GLFW.GLFW_KEY_LEFT, getActionList(Action.LEFT));
		buttonToActions.put(GLFW.GLFW_KEY_A, getActionList(Action.LEFT));
		buttonToActions.put(GLFW.GLFW_KEY_RIGHT, getActionList(Action.RIGHT));
		buttonToActions.put(GLFW.GLFW_KEY_D, getActionList(Action.RIGHT));
		
		buttonToActions.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, getActionList(Action.ACTION_1, Action.DRAG));
		buttonToActions.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, getActionList(Action.ACTION_2));
		buttonToActions.put(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, getActionList(Action.ACTION_3));
		
		generateActionToButton();
		
		System.out.println(buttonToActions);
		System.out.println(actionToButtons);
	}
	
	/**
	 * Use {@link #buttonToAction} to generate the contents of {@link #actionToButton}.
	 */
	private void generateActionToButton() {
		for (Entry<Integer, ArrayList<Action>> entry : buttonToActions.entrySet()) {
			for (Action action : entry.getValue()) {
				ArrayList<Integer> buttonsList = new ArrayList<Integer>();
				
				buttonsList.add(entry.getKey());
				if(actionToButtons.containsKey(action)) {
					buttonsList.addAll(actionToButtons.get(action));
				}
				
				actionToButtons.put(action, buttonsList);
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
	 * @return An {@code ArrayList} of {@link Action}s associated with the button, or an empty {@code ArrayList}.
	 */
	public ArrayList<Action> getActions(Integer button) {
		if(!buttonToActions.containsKey(button)) {
			return new ArrayList<Action>();
		}
		return buttonToActions.get(button);
	}
	
	/***
	 * @param action The action being targeted.
	 * @return An {@code ArrayList} of buttons associated with the {@link Action}, or an empty {@code ArrayList}.
	 */
	public ArrayList<Integer> getButtons(Action action) {
		if(!actionToButtons.containsKey(action)) {
			return new ArrayList<Integer>();
		}
		return actionToButtons.get(action);
	}
	
}
