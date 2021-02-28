/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.logging.Logger;

import org.dragonskulle.input.custom.MyActions;

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

    /** Stores the bindings between buttons and actions. */
    private Bindings mBindings;

    /** Allows button input to be detected. */
    @Getter private StoredButtons mButtons;

    /** Allows cursor position to be detected. */
    @Getter private Cursor mCursor;

    /** Allows scrolling to be detected. */
    @Getter private Scroll mScroll;

    /**
     * Create a new input manager.
     *
     * @param window A {@link Long} GLFW window id, or {@code null} if there is no window.
     * @param bindings A {@link CustomBindings} object that contains all the relevant button to action bindings.
     */
    public Input(Long window, CustomBindings bindings) {
        mBindings = new Bindings(bindings);

        mCursor = new Cursor();
        mButtons = new StoredButtons(mBindings);
        mScroll = new Scroll(mButtons);

        // If a window is provided, attach the event listeners.
        if (window != null) {
            mCursor.attachToWindow(window);
            mButtons.attachToWindow(window);
            mScroll.attachToWindow(window);
        } else {
        	LOGGER.warning("Input is not attatched to a window.");
        }

        // For infinite mouse movement.
        // GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        // For hiding the cursor.
        // GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    /**
     * Resets any recorded values ready for their new values.
     *
     * <p>Currently only used for {@link Scroll}.
     */
    public void reset() {
        mScroll.reset();
    }
}
