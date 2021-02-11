package org.dragonskulle.input;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * 
 * @author Craig Wilboure
 *	Input handling.
 */
public class Input {
	
	public static final Logger LOGGER = Logger.getLogger("input");
	
	private long window;
	
	public Input(long window) {
		LOGGER.log(Level.INFO, "Input constructor.");
		
		this.window = window;
		
		LOGGER.log(Level.INFO, "Window: " + window);
		
		GLFWKeyCallback keyboard = new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				System.out.println("Key pressed.");
			}
		};
		
		GLFW.glfwSetKeyCallback(window, keyboard);
		
		//glfwSetKeyCallback(window, keyboard);
	}
	
}
