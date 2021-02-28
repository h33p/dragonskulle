/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Manages all user input a window receives.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Access to whether {@link Action}s are active.
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

    /** Stores which actions are active. */
    private StoredActions mActions;

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
     */
    public Input(Long window) {
        MyBindings testBindings = new MyBindings();
        testBindings.add(GLFW.GLFW_KEY_E, ExtendedActions.DRAG);
        testBindings.add(GLFW.GLFW_KEY_Q, ExtendedActions.BONUS);
        
        testBindings.add(GLFW.GLFW_KEY_UP, ExtendedActions.UP, ExtendedActions.SCROLL_UP);
        testBindings.add(GLFW.GLFW_KEY_W, ExtendedActions.UP, ExtendedActions.SCROLL_UP);

        testBindings.add(GLFW.GLFW_KEY_DOWN, ExtendedActions.DOWN, ExtendedActions.SCROLL_DOWN);
        testBindings.add(GLFW.GLFW_KEY_S, ExtendedActions.DOWN, ExtendedActions.SCROLL_DOWN);

        testBindings.add(GLFW.GLFW_KEY_LEFT, ExtendedActions.LEFT);
        testBindings.add(GLFW.GLFW_KEY_A, ExtendedActions.LEFT);

        testBindings.add(GLFW.GLFW_KEY_RIGHT, ExtendedActions.RIGHT);
        testBindings.add(GLFW.GLFW_KEY_D, ExtendedActions.RIGHT);

        testBindings.add(GLFW.GLFW_MOUSE_BUTTON_LEFT, ExtendedActions.ACTION_1, ExtendedActions.DRAG);
        testBindings.add(GLFW.GLFW_MOUSE_BUTTON_RIGHT, ExtendedActions.ACTION_2);
        testBindings.add(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, ExtendedActions.ACTION_3);

        testBindings.add(Scroll.UP, ExtendedActions.SCROLL_UP, ExtendedActions.ZOOM_IN);
        testBindings.add(Scroll.DOWN, ExtendedActions.SCROLL_DOWN, ExtendedActions.ZOOM_OUT);
    	
    	mActions = new StoredActions();
        mBindings = new Bindings(testBindings);

        mCursor = new Cursor(mActions);
        mButtons = new StoredButtons(mActions, mBindings);
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
     *
     * <p>Currently only used for {@link Scroll}.
     */
    public void reset() {
        mScroll.reset();
    }
}
