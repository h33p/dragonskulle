package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.glfw.GLFW;

import lombok.Getter;

class StorageHandler {
	
	/** Key: Button <br/>
	 *  Value: {@link Action}s the Button activates.
	 * */
	@Getter private final HashMap<Integer, ArrayList<Action>> buttonToAction = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	@Getter private final HashMap<Action, ArrayList<Integer>> actionToButton = new HashMap<Action, ArrayList<Integer>>();
	
	public StorageHandler() {
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
}
