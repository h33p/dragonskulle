package org.dragonskulle.input;

import java.util.logging.Logger;

import lombok.Getter;

/**
 * Manages all user input a window receives.
 * <p>
 * Provides:
 * <ul>
 * <li> Access to whether {@link Action}s are active. </li>
 * <li> Access to the cursor. </li>
 * <li> Access to the raw scrolling value (via {@link #mScroll}). </li>
 * </ul>
 * 
 * @author Craig Wilboure
 */
public class Input {
	
	public static final Logger LOGGER = Logger.getLogger("input");		
	
	/** Stores the bindings between buttons and actions. */
	private Bindings mBindings;
	
	/** Stores which actions are active. */
	private Actions mActions;
	
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
		mActions = new Actions();
		mBindings = new Bindings();
		
		mCursor = new Cursor();
		mCursor.attachToWindow(window, mActions);
		
		mButtons = new Buttons();
		mButtons.attachToWindow(window, mActions, mBindings);
		
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
		return mActions.isActivated(action);
	}
	
	/**
	 * Resets any recorded values ready for their new values.
	 * <p>
	 * Currently only used for {@link Scroll}.
	 */
	public void reset() {
		mScroll.reset();
	}
}
