package org.dragonskulle.input;

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
	
	private long window;
	
	public Input(long window) {
		LOGGER.log(Level.INFO, "Input constructor.");
		
		this.window = window;
		
		LOGGER.log(Level.INFO, "Window: " + window);
		
		/*
		 * key: e.g. glfw.GLFW_KEY_W
		 * action: GLFW_PRESS, GLFW_REPEAT, GLFW_RELEASE
		 * mods: See modifier key flags (e.g. shift held).
		 */
		
		GLFWKeyCallback keyboard = new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				System.out.println("Key pressed.");
				System.out.println(String.format("key: %d\nscancode: %d\naction: %d\nmods: %d", key, scancode, action, mods));
			}
		};
		
		GLFWMouseButtonCallback mouseButton = new GLFWMouseButtonCallback() {
			
			@Override
			public void invoke(long window, int button, int action, int mods) {
				System.out.println("Mouse button pressed.");
				System.out.println(String.format("button: %d\naction: %d\nmods: %d", button, action, mods));
			}
		};
		
		GLFWCursorPosCallback mousePosition = new GLFWCursorPosCallback() {
			
			@Override
			public void invoke(long window, double xpos, double ypos) {
				// TODO Auto-generated method stub
				
			}
		};
		
		GLFWScrollCallback scroll = new GLFWScrollCallback() {
			
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				// TODO Auto-generated method stub
				
			}
		};
		
		GLFW.glfwSetKeyCallback(window, keyboard);
		GLFW.glfwSetMouseButtonCallback(window, mouseButton);
		GLFW.glfwSetCursorPosCallback(window, mousePosition);
		GLFW.glfwSetScrollCallback(window, scroll);
		
		//glfwSetKeyCallback(window, keyboard);
	}
	
}
