package org.dragonskulle.input;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

/**
 * 
 * @author Craig Wilboure
 *	Input handling.
 */
public class Input {
	
	public static final Logger LOGGER = Logger.getLogger("input");	
	
	private long window;
	
	
	/** Convert between buttons and actions. */
	private Converter converter = new Converter();
	
	private Actions actions = new Actions();
	private Buttons buttons = new Buttons(converter, actions);
	
	private Scroll scroll = new Scroll();
	
	public Input(long window) {
		LOGGER.log(Level.INFO, "Input constructor.");
		
		this.window = window;

		new KeyboardListener(window, buttons);
		new MouseButtonListener(window, buttons);
		new MouseScrollListener(window, buttons, scroll);
		
		
		GLFWCursorPosCallback mousePosition = new GLFWCursorPosCallback() {
			
			@Override
			public void invoke(long window, double xpos, double ypos) {
				//System.out.println("Mouse position.");
				//System.out.println(String.format("xpos: %f\nypos: %f", xpos, ypos));
			}
		};
		
		GLFW.glfwSetCursorPosCallback(window, mousePosition);
		
		// For infinite mouse movement.
		// GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		// For hiding the cursor.
		// GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
	}
	
	public boolean isActivated(Action action) {
		return actions.isActivated(action);
	}
	
	public void resetScroll() {
		scroll.reset();
		buttons.released(Scroll.UP);
		buttons.released(Scroll.DOWN);
	}
	
	public double getScroll() {
		return scroll.getAmount();
	}
}
