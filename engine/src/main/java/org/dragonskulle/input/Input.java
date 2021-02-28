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
    @Getter
    private Buttons mButtons;
    
    /**
     * Create a new input manager. If a window is present, user input from that window is monitored and this is reflected in the relevant {@link Actions}.
     * <p>
     * Automatically submits any bindings ({@link Bindings#submit()}).
     *
     * @param window A {@link Long} GLFW window id, or {@code null} if there is no window.
     * @param bindings A {@link BindingsTemplate} object that contains all the relevant button to action bindings.
     */
    public Input(Long window, Bindings bindings) {
    	// Generate all the bindings from the bindingsTemplate.
    	//Bindings bindings = new Bindings(bindingsTemplate);

    	bindings.submit();
        
        mButtons = new Buttons(bindings);
        
        Actions.cursor = new Cursor();
        Actions.scroll = new Scroll(mButtons);

        // If a window is provided, attach the event listeners.
        if (window != null) {
        	mButtons.attachToWindow(window);
        	Actions.cursor.attachToWindow(window);
            Actions.scroll.attachToWindow(window);
        } else {
        	LOGGER.warning("Input is not attatched to a window.");
        }

        // For infinite mouse movement.
        // GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        // For hiding the cursor.
        // GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }
}
