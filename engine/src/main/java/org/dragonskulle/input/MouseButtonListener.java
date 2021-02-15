package org.dragonskulle.input;

import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

/**
 * Adds a {@code GLFWMouseButtonCallback}.
 * 
 * @author Craig Wilbourne
 */
class MouseButtonListener {
	
	public static final Logger LOGGER = Logger.getLogger("MouseButtonListener");
	
	private Buttons buttons;
	
	public MouseButtonListener(long window, Buttons buttons) {
		this.buttons = buttons;
		
		GLFW.glfwSetMouseButtonCallback(window, create());
	}

	private GLFWMouseButtonCallback create() {
		
		 return new GLFWMouseButtonCallback() {
			
			@Override
			public void invoke(long window, int button, int action, int mods) {
				// LOGGER.info(String.format("button: %d\naction: %d\nmods: %d", button, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					buttons.pressed(button);
				} else if(action == GLFW.GLFW_RELEASE) {
					buttons.released(button);
				}
			}
		};
	}	
}
