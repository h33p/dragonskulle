package org.dragonskulle.input.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.dragonskulle.input.Action;
import org.lwjgl.glfw.GLFW;

/**
 * Converts between which buttons and {@link Action}s.
 * 
 * @author Craig Wilbourne
 */
public class Converter {
	
	/** Key: Button <br/>
	 *  Value: {@link Action}s the Button activates.
	 * */
	private final HashMap<Integer, ArrayList<Action>> buttonToAction = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	private final HashMap<Action, ArrayList<Integer>> actionToButton = new HashMap<Action, ArrayList<Integer>>();
	
	public Converter() {
		// Currently hard-coded values:
		
		buttonToAction.put(Scroll.UP, getActionList(Action.SCROLL_UP, Action.ZOOM_IN));
		buttonToAction.put(Scroll.DOWN, getActionList(Action.SCROLL_DOWN, Action.ZOOM_OUT));
		
		buttonToAction.put(GLFW.GLFW_KEY_UP, getActionList(Action.UP, Action.SCROLL_UP));
		buttonToAction.put(GLFW.GLFW_KEY_W, getActionList(Action.UP, Action.SCROLL_UP));
		buttonToAction.put(GLFW.GLFW_KEY_DOWN, getActionList(Action.DOWN, Action.SCROLL_DOWN));
		buttonToAction.put(GLFW.GLFW_KEY_S, getActionList(Action.DOWN, Action.SCROLL_DOWN));
		
		buttonToAction.put(GLFW.GLFW_KEY_LEFT, getActionList(Action.LEFT));
		buttonToAction.put(GLFW.GLFW_KEY_A, getActionList(Action.LEFT));
		buttonToAction.put(GLFW.GLFW_KEY_RIGHT, getActionList(Action.RIGHT));
		buttonToAction.put(GLFW.GLFW_KEY_D, getActionList(Action.RIGHT));
		
		buttonToAction.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, getActionList(Action.ACTION_1, Action.DRAG));
		buttonToAction.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, getActionList(Action.ACTION_2));
		buttonToAction.put(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, getActionList(Action.ACTION_3));
		
		//buttonToAction.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, getActionList(Action.MOUSE_LEFT));
		//buttonToAction.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, getActionList(Action.MOUSE_RIGHT));
		//buttonToAction.put(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, getActionList(Action.MOUSE_MIDDLE));
		
		generateActionToButton();
	}
	
	/**
	 * Use {@link #buttonToAction} to generate the contents of {@link #actionToButton}.
	 */
	private void generateActionToButton() {
		for (Entry<Integer, ArrayList<Action>> entry : buttonToAction.entrySet()) {
			for (Action action : entry.getValue()) {
				ArrayList<Integer> buttonsList = new ArrayList<Integer>();
				
				buttonsList.add(entry.getKey());
				if(actionToButton.containsKey(action)) {
					buttonsList.addAll(actionToButton.get(action));
				}
				
				actionToButton.put(action, buttonsList);
			}
		}
	}
	
	/**
	 * Temporary function to easily generate an ArrayList of {@link Action}s.
	 * TODO: Remove.
	 * @param selectedActions
	 * @return
	 */
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
		if(!buttonToAction.containsKey(button)) {
			return new ArrayList<Action>();
		}
		return buttonToAction.get(button);
	}
	
	/***
	 * @param action The action being targeted.
	 * @return An {@code ArrayList} of buttons associated with the {@link Action}, or an empty {@code ArrayList}.
	 */
	public ArrayList<Integer> getButtons(Action action) {
		if(!actionToButton.containsKey(action)) {
			return new ArrayList<Integer>();
		}
		return actionToButton.get(action);
	}
	
}
