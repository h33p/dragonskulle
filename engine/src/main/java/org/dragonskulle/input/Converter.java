package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.glfw.GLFW;

import lombok.Getter;

class Converter {
	
	/** Key: Button <br/>
	 *  Value: {@link Action}s the Button activates.
	 * */
	private final HashMap<Integer, ArrayList<Action>> buttonToAction = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	private final HashMap<Action, ArrayList<Integer>> actionToButton = new HashMap<Action, ArrayList<Integer>>();
	
	public Converter() {
		buttonToAction.put(GLFW.GLFW_KEY_UP, getActionList(Action.UP, Action.ACTION_1));
		buttonToAction.put(GLFW.GLFW_KEY_W, getActionList(Action.UP));
		
		
		
		generateActionToButton();
	}
	
	/** Using {@link #buttonToAction} generate the contents of {@link #actionToButton}*/
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
	
	// Temporary function to generate an arraylist of actions.
	private ArrayList<Action> getActionList(Action... selectedActions){
		ArrayList<Action> list = new ArrayList<Action>();
		
		for (Action action : selectedActions) {
			list.add(action);
		}
		
		return list;
	}
	
	/**
	 * @param button
	 * @return The {@link Action}s associated with the button.
	 */
	public ArrayList<Action> getActions(Integer button) {
		if(!buttonToAction.containsKey(button)) {
			return new ArrayList<Action>();
		}
		return buttonToAction.get(button);
	}
	
	/***
	 * @param action
	 * @return The buttons associated with the {@link Action}.
	 */
	public ArrayList<Integer> getButtons(Action action) {
		if(!actionToButton.containsKey(action)) {
			return new ArrayList<Integer>();
		}
		return actionToButton.get(action);
	}
	
}
