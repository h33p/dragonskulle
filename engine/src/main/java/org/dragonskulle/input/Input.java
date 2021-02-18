package org.dragonskulle.input;

import java.util.logging.Logger;

import lombok.Getter;

/**
 * Manages all user input a window receives.
 * <p>
 * Provides:
 * <ul>
 * <li>Access to whether {@link Action}s are active.</li>
 * <li>Access to the cursor.</li>
 * </ul>
 * 
 * @author Craig Wilboure
 */
public class Input {
	
	public static final Logger LOGGER = Logger.getLogger("input");		
	
	/** Stores the bindings between buttons and actions. */
	private Bindings bindings;
	
	/** Stores which actions are active. */
	private Actions actions;
	
	/** Allows button input to be detected. */
	@Getter
	private Buttons mButtons;
	
	/** Allows cursor position to be detected. */
	@Getter
	private Cursor mCursor;
	
	/** Allows scrolling to be detected. */
	@Getter
	private Scroll mScroll = new Scroll();
	
	
	
	public Input(long window) {
		actions = new Actions();
		bindings = new Bindings();
		
		mCursor = new Cursor();
		mCursor.attachToWindow(window, actions);
		
		mButtons = new Buttons();
		mButtons.attachToWindow(window, actions, bindings);
		
		mScroll = new Scroll();
		mScroll.attachToWindow(window, mButtons);
		
		//new MouseScrollListener(window, buttons, scroll);		
		
		// For infinite mouse movement.
		// GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		// For hiding the cursor.
		// GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
	}
	
	/**
	 * Query whether an {@link Action} is activated.
	 * 
	 * @param action The action to be tested.
	 * @return {@code true} if the action is activated, otherwise {@code false}.
	 */
	public boolean isActivated(Action action) {
		return actions.isActivated(action);
	}
	
	/**
	 * Resets the {@link #scroll} detection for the next input polling cycle.
	 *
	public void resetScroll() {
		//scroll.reset();
		//buttons.released(Scroll.UP);
		//buttons.released(Scroll.DOWN);
	}
	*/
	
	/*
	 * 
	 * @return The amount of scrolling done since {@link Scroll#reset} was called.
	 *
	public double getScroll() {
		return scroll.getAmount();
	}
	*/
}
