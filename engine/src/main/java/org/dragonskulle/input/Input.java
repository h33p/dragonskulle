/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.logging.Logger;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Listens to all user input for a window, and reflects it in the relevant {@link Actions}.
 *
 * @author Craig Wilboure
 */
@Accessors(prefix = "m")
public class Input {

    /** Used to log messages. */
    private static final Logger LOGGER = Logger.getLogger("input");

    /** Allows button presses to trigger actions. */
    private static Buttons mButtons;
    
    /** Allows bindings between buttons and actions. */
    @Getter
    private static Bindings mBindings;
    
    /**
     * 
     * 
     * @param window The window Input should listen to, or {@code null} if no window is to be connected.
     * @param bindings The button bindings that map buttons to events.
     */
    public static void initialise(Long window, Bindings bindings) {
    	// Store the bindings.
    	mBindings = bindings;
    	// Submit the current bindings.
    	bindings.submit();
    	
    	// Detect buttons based on the bindings.
    	mButtons = new Buttons(bindings);
        // Detect cursor location.
        Actions.cursor = new Cursor();
        // Detect scrolling, triggering the buttons Scroll.UP and Scroll.DOWN.
        Actions.scroll = new Scroll(mButtons);
    	
        // If a window is provided, attach the event listeners.
        if (window != null) {
        	mButtons.attachToWindow(window);
        	Actions.cursor.attachToWindow(window);
            Actions.scroll.attachToWindow(window);
        } else {
        	LOGGER.warning("Input is not attatched to a window.");
        }
    }
    
    /**
     * Get {@link #mButtons}, which stores which buttons are pressed and triggers their actions.
     * 
     * @return The buttons manager.
     */
    static Buttons getButtons() {
    	return mButtons;
    }
}
