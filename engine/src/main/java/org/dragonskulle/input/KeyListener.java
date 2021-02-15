package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

class KeyListener {

	public static final Logger LOGGER = Logger.getLogger("KeyListener");
	
	private Converter converter;	
	private Buttons buttons;
	private Actions actions;
	
	public KeyListener(long window, Converter converter, Buttons buttons, Actions actions) {
		this.converter = converter;
		this.buttons = buttons;
		this.actions = actions;
		
		GLFW.glfwSetKeyCallback(window, create());
	}

	private GLFWKeyCallback create() {
		return new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				//LOGGER.info(String.format("key: %d\nscancode: %d\naction: %d\nmods: %d", key, scancode, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					keyDown(key);					
				} else if(action == GLFW.GLFW_RELEASE) {
					keyUp(key);
				}
				
				//LOGGER.info(actions.toString());
			}
		};
	}
	
	private void keyDown(Integer key) {
		buttons.setActivated(key, true);
		
		ArrayList<Action> selectedActions = converter.getActions(key);
		
		for (Action selectedAction : selectedActions) {
			actions.setActivated(selectedAction, true);
		}
	}
	
	private void keyUp(Integer key) {
		buttons.setActivated(key, false);
		
		ArrayList<Action> selectedActions = converter.getActions(key);
		
		for (Action selectedAction : selectedActions) {
			ArrayList<Integer> selectedButtons = converter.getButtons(selectedAction);
			
			Boolean remove = true;
			for(Integer selectedButton : selectedButtons) {
				if(buttons.contains(selectedButton) && buttons.isActivated(selectedButton) == true) {
					// Another button is currently activated, so do not set action to false.
					remove = false;
					break;
				}
			}
			
			if(remove) {
				actions.setActivated(selectedAction, false);
				System.out.print("REMOVED");
			}
		}
	}
	
}
