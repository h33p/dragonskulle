package org.dragonskulle.input;

import java.util.logging.Logger;

import org.dragonskulle.input.listeners.MouseScrollListener;
import org.dragonskulle.input.storage.Scroll;

import lombok.Getter;

/**
 * Manages all user input.
 * @author Craig Wilboure
 */
public class Input {
	
	public static final Logger LOGGER = Logger.getLogger("input");		
	
	
	/** Store triggered actions. */
	private Actions actions = new Actions();
	/** Store triggered buttons. */
	//private Buttons buttons = new Buttons(converter, actions);
	/** Store mouse scroll movement. */
	private Scroll scroll = new Scroll();
	/** Store mouse position. */
	//private MousePosition mousePosition = new MousePosition();
	
	@Getter
	private Cursor cursor;
	
	@Getter
	private Buttons buttons;
	
	/** Stores the bindings between buttons and actions. */
	private Bindings bindings;
	
	public Input(long window) {
		bindings = new Bindings();
		
		cursor = new Cursor();
		cursor.attachToWindow(window, actions);
		
		buttons = new Buttons();
		buttons.attachToWindow(window, actions, bindings);
		
		//new KeyboardListener(window, buttons);
		//new MouseButtonListener(window, buttons);
		//new MouseScrollListener(window, buttons, scroll);		
		
		// For infinite mouse movement.
		// GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		// For hiding the cursor.
		// GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
	}
	
	/**
	 * Query whether an {@link Action} is activated.
	 * @param action The action to be tested.
	 * @return {@code true} if the action is activated, otherwise {@code false}.
	 */
	public boolean isActivated(Action action) {
		return actions.isActivated(action);
	}
	
	/**
	 * Resets the {@link #scroll} detection for the next input polling cycle.
	 */
	public void resetScroll() {
		//scroll.reset();
		//buttons.released(Scroll.UP);
		//buttons.released(Scroll.DOWN);
	}
	
	/**
	 * 
	 * @return The amount of scrolling done since {@link Scroll#reset} was called.
	 */
	public double getScroll() {
		return scroll.getAmount();
	}
	
}
