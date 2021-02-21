package org.dragonskulle.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallback;

/**
 * Once attached to a window, this allows for scrolling to:
 * <ul>
 * <li> Trigger the {@link Scroll#UP} button (which can be used to trigger {@link Action}s). </li>
 * <li> Trigger the {@link Scroll#DOWN} button (which can be used to trigger {@link Action}s). </li>
 * <li> Be accessed as a raw value. </li>
 * </ul>
 * 
 * @author Craig Wilbourne
 */
public class Scroll {

	/** Button code for scrolling up.
	 * <p>
	 * Allows scrolling to be treaded as a button press- meaning this button code can be used to trigger {@link Action}s.
	 * <p>
	 * This is an arbitrarily chosen value that is not shared by other GLFW keys.
	 */
	public static final Integer UP = -777;
	
	/** Button code for scrolling down.
	 * <p>
	 * Allows scrolling to be treaded as a button press- meaning this button code can be used to trigger {@link Action}s.
	 * <p>
	 * This is an arbitrarily chosen value that is not shared by other GLFW keys.
	 */
	public static final Integer DOWN = -776;
	
	/** The amount of scrolling done since the last call to {@link #reset()}. Depending on direction scrolled, this value is negative or positive. */
	private double mAmount = 0;
	
	/** Allows buttons to be pressed and released. */
	private Buttons mButtons;
	
	/**
	 * Listens for GLFW scrolling changes and stores these changes as well as simulating button presses for either scrolling up or down.
	 */
	private class ScrollListener extends GLFWScrollCallback {
		@Override
		public void invoke(long window, double xOffset, double yOffset) {			
			// Store the actual scroll value.
			add(yOffset);
			
			// Also trigger a button press.
			// Allows for scrolling to be treated as a button press, which can then be binded Actions.
			if(yOffset > 0) {
				mButtons.press(Scroll.UP);
			} else {
				mButtons.press(Scroll.DOWN);
			}
		}
	}
	
	/**
	 * Attach the scrolling detection to the window.
	 * <p>
	 * Required to allow scrolling to be detected both as a direct value and via {@link Actions}.
	 * 
	 * @param window The window to attach to.
	 * @param buttons The buttons that will be pressed and released.
	 */
	void attachToWindow(long window, Buttons buttons) {
		mButtons = buttons;
		
		// Set the listener.
		GLFW.glfwSetScrollCallback(window, new ScrollListener());
	}
	
	/**
	 * Get the scrolling amount since the last {@link Scroll#reset()}.
	 * 
	 * @return The amount of scrolling.
	 */
	public double getAmount() {
		return mAmount;
	}
	
	/**
	 * Resets scrolling previously recorded, ready for new scrolling to be recorded. 
	 * <p>
	 * Needs to be called frequently so:
	 * <ul>
	 * <li> Changes in scrolling direction are represented in {@link #mAmount}. </li>
	 * <li> Stopping scrolling is represented in {@link #mAmount} and {@link #mButtons}. </li>
	 * </ul>
	 */
	void reset() {
		mAmount = 0;
		mButtons.release(Scroll.UP);
		mButtons.release(Scroll.DOWN);
	}
	
	/**
	 * Add to the total amount of scrolling done.
	 * 
	 * @param value The value to add.
	 */
	void add(double value) {
		mAmount += value;
	}
	
}
