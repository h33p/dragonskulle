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
	
	
	
	/** Key: Button <br/>
	 *  Value: {@link Action}s the Button activates.
	 * */
	//private static final HashMap<Integer, ArrayList<Action>> buttonToAction = new HashMap<Integer, ArrayList<Action>>();
	
	/** Key: {@link Action} <br/>
	 *  Value: Buttons that activate the Action.
	 * */
	//private static final HashMap<Action, ArrayList<Integer>> actionToButton = new HashMap<Action, ArrayList<Integer>>();
	
	private long window;
	
	private Actions actions = new Actions();
	private Buttons buttons = new Buttons();
	
	/** Convert between buttons and actions. */
	private Converter converter = new Converter();
	
	public Input(long window) {
		LOGGER.log(Level.INFO, "Input constructor.");
		
		this.window = window;
		
		LOGGER.log(Level.INFO, "Window: " + window);
		

		//buttonToAction.put(265, getActionList(Action.UP, Action.ACTION_1));
		//buttonToAction.put(87, getActionList(Action.UP));
		
		
		//LOGGER.info(converter.getActionToButton().toString());
		//LOGGER.info(converter.getButtonToAction().toString());
		
		/*
		 * key: e.g. glfw.GLFW_KEY_W
		 * action: GLFW_PRESS, GLFW_REPEAT, GLFW_RELEASE
		 * mods: See modifier key flags (e.g. shift held).
		 */
		
		KeyListener keyListener = new KeyListener(window, converter, buttons, actions);
		
		GLFWMouseButtonCallback mouseButton = new GLFWMouseButtonCallback() {
			
			@Override
			public void invoke(long window, int button, int action, int mods) {
				//System.out.println(String.format("button: %d\naction: %d\nmods: %d", button, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					//buttons.put(button, true);
					buttons.setActivated(button, true);
				} else if(action == GLFW.GLFW_RELEASE) {
					//buttons.put(button, false);
					buttons.setActivated(button, false);
				}
				
				//LOGGER.info(actions.toString());
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
		
		
		GLFW.glfwSetMouseButtonCallback(window, mouseButton);
		GLFW.glfwSetCursorPosCallback(window, mousePosition);
		GLFW.glfwSetScrollCallback(window, scroll);
		
		// For infinite mouse movement.
		// GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		// For hiding the cursor.
		// GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
	}
	
	public boolean isActivated(Action action) {
		return actions.isActivated(action);
	}
}
