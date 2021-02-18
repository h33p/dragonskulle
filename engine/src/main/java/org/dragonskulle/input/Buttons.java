package org.dragonskulle.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

/**
 * Stores GLFW keyboard keys (as {@link Integer}s) and whether they are activated.
 * 
 * @author Craig Wilbourne
 */
public class Buttons extends Activatable<Integer>{

	private Actions mActions;
	private Bindings mBindings;
	
	private class KeyboardListener extends GLFWKeyCallback {
		
		@Override
		public void invoke(long window, int button, int scancode, int action, int mods) {
			if(action == GLFW.GLFW_PRESS) {
				press(button);
			} else if(action == GLFW.GLFW_RELEASE) {
				release(button);
			}
		}
		
	}
	
	private class MouseListener extends GLFWMouseButtonCallback {

		@Override
		public void invoke(long window, int button, int action, int mods) {
			if(action == GLFW.GLFW_PRESS) {
				press(button);
			} else if(action == GLFW.GLFW_RELEASE) {
				release(button);
			}
		}
		
	}
	
	
	void attachToWindow(long window, Actions actions, Bindings bindings) {
		mActions = actions;
		mBindings = bindings;
		
		GLFW.glfwSetKeyCallback(window, new KeyboardListener());
		GLFW.glfwSetMouseButtonCallback(window, new MouseListener());
	}

	private void press(int button) {
		setActivated(button, true);
		
		for (Action action : mBindings.getActions(button)) {
			mActions.setActivated(action, true);
		}
	}
	
	private void release(int button) {
		setActivated(button, false);
		
		// Check each action the button triggers, deactivating each action if no other buttons are currently triggering it.
		for (Action action : mBindings.getActions(button)) {			
			Boolean deactivate = true;
			
			for(Integer otherButton : mBindings.getButtons(action)) {
				// If another button that triggers the action is currently activated, do not set action to false.
				if(isActivated(otherButton) == true) {
					deactivate = false;
					break;
				}
			}
			
			// If no other buttons are triggering the action, deactivate the action.
			if(deactivate) {
				mActions.setActivated(action, false);
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//private Converter converter;
	//private Actions actions;
	
	/*
	public Buttons(Converter converter, Actions actions) {
		this.converter = converter;
		this.actions = actions;
	}
	*/
	
	/**
	 * When a button has been pressed, it correctly activates any {@link Action}s.
	 * @param button The button that has been pressed.
	 */
	/*
	public void pressed(Integer button) {
		setActivated(button, true);
		
		for (Action action : converter.getActions(button)) {
			actions.setActivated(action, true);
		}
	}
	*/
	
	/**
	 * When a button has been released, it correctly deactivates any {@link Action}s.
	 * @param button The button that has been released.
	 */
	/*
	public void released(Integer button) {
		setActivated(button, false);
		
		// Check each action the button triggers, deactivating each action if no other buttons are currently triggering it.
		for (Action action : converter.getActions(button)) {			
			Boolean deactivate = true;
			
			for(Integer otherButton : converter.getButtons(action)) {
				// If another button that triggers the action is currently activated, do not set action to false.
				if(isActivated(otherButton) == true) {
					deactivate = false;
					break;
				}
			}
			
			// If no other buttons are triggering the action, deactivate the action.
			if(deactivate) {
				actions.setActivated(action, false);
			}
		}
	}
	*/
	
}
