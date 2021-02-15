package org.dragonskulle.input;

import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

/**
 * Adds a {@code GLFWScrollCallback}.
 * 
 * @author Craig Wilbourne
 */
class MouseScrollListener {
	
	public static final Logger LOGGER = Logger.getLogger("MouseScrollListener");
	
	private Buttons buttons;
	
	public MouseScrollListener(long window, Buttons buttons) {
		this.buttons = buttons;
		
		GLFW.glfwSetScrollCallback(window, create());
	}

	private GLFWScrollCallback create() {
		return new GLFWScrollCallback() {
			
			@Override
			public void invoke(long window, double xOffset, double yOffset) {
				//LOGGER.info(String.format("xOffset: %f\nyOffset: %f", xOffset, yOffset));
				
				if(yOffset > 0) {
					// Scrolling up.
					buttons.pressed(Scroll.UP);
				} else { // if(yOffset < 0) 
					// Scrolling down.
					buttons.pressed(Scroll.DOWN);
				}
			}
		};
	}	
}
