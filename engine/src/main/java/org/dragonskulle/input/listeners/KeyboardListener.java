package org.dragonskulle.input.listeners;

import java.util.logging.Logger;

import org.dragonskulle.input.storage.Buttons;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * Adds a {@code GLFWKeyCallback}.
 * 
 * @author Craig Wilbourne
 */
public class KeyboardListener {

	public static final Logger LOGGER = Logger.getLogger("KeyboardListener");
	
	private Buttons buttons;
	
	public KeyboardListener(long window, Buttons buttons) {
		this.buttons = buttons;
		
		GLFW.glfwSetKeyCallback(window, create());
	}

	private GLFWKeyCallback create() {
		return new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				//LOGGER.info(String.format("key: %d\nscancode: %d\naction: %d\nmods: %d", key, scancode, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					buttons.pressed(key);
				} else if(action == GLFW.GLFW_RELEASE) {
					buttons.released(key);
				}
			}
		};
	}	
}
