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
	
	private Scroll scroll;
	private Buttons buttons;
	
	public MouseScrollListener(long window, Buttons buttons, Scroll scroll) {
		this.scroll = scroll;
		this.buttons = buttons;
		
		GLFW.glfwSetScrollCallback(window, create());
	}

	private GLFWScrollCallback create() {
		return new GLFWScrollCallback() {
			
			@Override
			public void invoke(long window, double xOffset, double yOffset) {
				//LOGGER.info(String.format("xOffset: %f\nyOffset: %f", xOffset, yOffset));
				
				// Store the actual scroll value.
				scroll.add(yOffset);
				
				// Treat the scrolling as a button press. 
				if(yOffset > 0) {
					buttons.pressed(Scroll.UP);
				} else {
					buttons.pressed(Scroll.DOWN);
				}
			}
		};
	}	
}
