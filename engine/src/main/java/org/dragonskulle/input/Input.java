package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

/**
 * 
 * @author Craig Wilboure
 *	Input handling.
 */
public class Input {
	
	public static final Logger LOGGER = Logger.getLogger("input");
	
	/** Stores whether keyboard and mouse buttons are pressed or released.*/
	private static final HashMap<Integer, Boolean> buttons = new HashMap<Integer, Boolean>();
	
	
	/** Stores whether a specific {@link Action} is being activated. */
	private static final HashMap<Action, Boolean> actions = new HashMap<Action, Boolean>();
	
	/** Key: Button <br/>
	 *  Value: {@link Action}s the Button activates.
	 * */
	private static final HashMap<Integer, ArrayList<Action>> buttonToAction = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	private static final HashMap<Action, ArrayList<Integer>> actionToButton = new HashMap<Action, ArrayList<Integer>>();
	
	private long window;
	
	public Input(long window) {
		LOGGER.log(Level.INFO, "Input constructor.");
		
		this.window = window;
		
		LOGGER.log(Level.INFO, "Window: " + window);
		

		buttonToAction.put(265, getActionList(Action.UP, Action.ACTION_1));
		buttonToAction.put(87, getActionList(Action.UP));
		
		
		for (Entry<Integer, ArrayList<Action>> entry : buttonToAction.entrySet()) {
			//actionToButton.put(entry.getValue(), entry.getKey());
			for (Action action : entry.getValue()) {
				ArrayList<Integer> buttonsList = new ArrayList<Integer>();
				
				buttonsList.add(entry.getKey());
				if(actionToButton.containsKey(action)) {
					buttonsList.addAll(actionToButton.get(action));
				}
				
				actionToButton.put(action, buttonsList);
			}
		}
		
		LOGGER.info(buttonToAction.toString());
		LOGGER.info(actionToButton.toString());
		
		
		
		/*
		 * key: e.g. glfw.GLFW_KEY_W
		 * action: GLFW_PRESS, GLFW_REPEAT, GLFW_RELEASE
		 * mods: See modifier key flags (e.g. shift held).
		 */
		
		GLFWKeyCallback keyboard = new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				//LOGGER.info(String.format("key: %d\nscancode: %d\naction: %d\nmods: %d", key, scancode, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					buttons.put(key, true);
					
					if(buttonToAction.containsKey(key)) {
						ArrayList<Action> selectedActions = buttonToAction.get(key);
						
						for (Action selectedAction : selectedActions) {
							actions.put(selectedAction, true);
						}						
					}
					
				} else if(action == GLFW.GLFW_RELEASE) {
					buttons.put(key, false);
					
					if(buttonToAction.containsKey(key)) {
						ArrayList<Action> selectedActions = buttonToAction.get(key);
						
						for (Action selectedAction : selectedActions) {
							ArrayList<Integer> buttonsCauseAction = actionToButton.get(selectedAction);
							if(buttonsCauseAction == null) {
								LOGGER.severe("buttonsCauseAction is null!");
								return;
							}
							
							Boolean remove = true;
							for(Integer selectedButton : buttonsCauseAction) {
								if(buttons.containsKey(selectedButton) && buttons.get(selectedButton) == true) {
									// Another button is currently activated, so do not set action to false.
									remove = false;
									break;
								}
							}
							
							if(remove) {
								actions.put(selectedAction, false);
							}
						}
						
						/*
						ArrayList<Action> selectedActions = buttonToAction.get(key);
						
						for (Action selectedAction : selectedActions) {
							actions.put(selectedAction, false);
						}
						*/						
					}
				}
				
				LOGGER.info(actions.toString());
			}
		};
		
		GLFWMouseButtonCallback mouseButton = new GLFWMouseButtonCallback() {
			
			@Override
			public void invoke(long window, int button, int action, int mods) {
				//System.out.println(String.format("button: %d\naction: %d\nmods: %d", button, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					buttons.put(button, true);
				} else if(action == GLFW.GLFW_RELEASE) {
					buttons.put(button, false);
				}
				
				LOGGER.info(actions.toString());
			}
		};
		
		GLFWCursorPosCallback mousePosition = new GLFWCursorPosCallback() {
			
			@Override
			public void invoke(long window, double xpos, double ypos) {
				//System.out.println("Mouse position.");
				//System.out.println(String.format("xpos: %f\nypos: %f", xpos, ypos));
			}
		};
		
		GLFWScrollCallback scroll = new GLFWScrollCallback() {
			
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				//System.out.println("Mouse scroll.");
				//System.out.println(String.format("xoffset: %f\nyoffset: %f", xoffset, yoffset));
			}
		};
		
		GLFW.glfwSetKeyCallback(window, keyboard);
		GLFW.glfwSetMouseButtonCallback(window, mouseButton);
		GLFW.glfwSetCursorPosCallback(window, mousePosition);
		GLFW.glfwSetScrollCallback(window, scroll);
		
		// For infinite mouse movement.
		// GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		// For hiding the cursor.
		// GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
	}
	
	private ArrayList<Action> getActionList(Action... selectedActions){
		ArrayList<Action> list = new ArrayList<Action>();
		
		for (Action action : selectedActions) {
			list.add(action);
		}
		
		return list;
	}
	
}
