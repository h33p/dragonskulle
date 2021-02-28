/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.logging.Logger;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Once initialised for a specific window, input will listen to all user input and reflect this in the relevant {@link Actions}.
 *
 * @author Craig Wilboure
 */
@Accessors(prefix = "s")
public class Input {

    /** Used to log messages. */
    private static final Logger LOGGER = Logger.getLogger("input");

    /** Allows button presses to trigger actions. */
    private static Buttons sButtons;
    
    /** Allows bindings between buttons and actions. */
    @Getter
    private static Bindings sBindings;
    
    /**
     * Starts the detection of buttons, the cursor and the mouse scroll wheel for a specified window.
     * <b>
     * Automatically submits the current bindings for use.
     * 
     * @param window The window Input should listen to, or {@code null} if no GLFW window is to be connected.
     * @param bindings The button bindings that map buttons to events.
     */
    public static void initialise(Long window, Bindings bindings) {
    	// Store the bindings.
    	sBindings = bindings;
    	// Submit the current bindings.
    	bindings.submit();
    	
    	// Detect buttons based on the bindings.
    	sButtons = new Buttons(bindings);
        // Detect cursor location.
        Actions.setCursor(new Cursor());
        // Detect scrolling, triggering the buttons Scroll.UP and Scroll.DOWN.
        Actions.setScroll(new Scroll(sButtons));
    	
        // If a window is provided, attach the event listeners.
        if (window != null) {
        	sButtons.attachToWindow(window);
        	Actions.getCursor().attachToWindow(window);
            Actions.getScroll().attachToWindow(window);
        } else {
        	LOGGER.warning("Input is not attatched to a window.");
        }
    }
    
    /**
     * Get {@link #sButtons}, which stores which buttons are pressed and triggers their actions.
     * 
     * @return The buttons manager.
     */
    static Buttons getButtons() {
    	return sButtons;
    }
}
