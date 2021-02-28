/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.logging.Logger;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Manages all user input a window receives.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Access to the cursor.
 *   <li>Access to the raw scrolling value (via {@link #mScroll}).
 * </ul>
 *
 * @author Craig Wilboure
 */
@Accessors(prefix = "m")
public class Input {

    /** Used to log messages. */
    private static final Logger LOGGER = Logger.getLogger("input");

    /** Allows button input to be detected. */
    @Getter private Buttons mButtons;

    /**
     * Create a new input manager.
     *
     * @param window A {@link Long} GLFW window id, or {@code null} if there is no window.
     * @param bindings A {@link BindingsTemplate} object that contains all the relevant button to action bindings.
     */
    public Input(Long window, BindingsTemplate bindingsTemplate) {
    	Bindings bindings = new Bindings(bindingsTemplate);

        
        
        mButtons = new Buttons(bindings);
        
        Actions.cursor = new Cursor();
        Actions.scroll = new Scroll(mButtons);

        // If a window is provided, attach the event listeners.
        if (window != null) {
        	Actions.cursor.attachToWindow(window);
            mButtons.attachToWindow(window);
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
