/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

/**
 * Once attached to a window, this allows for buttons to trigger actions, as defined by the mapping
 * in {@link Bindings}.
 *
 * @author Craig Wilbourne
 */
public class Buttons extends Activatable<Integer> {

    /** Allows {@link Action}s to be activated and deactivated. */
    private Actions mActions;
    /** Allows the mapping between buttons and actions to be accessed. */
    private Bindings mBindings;

    /**
     * Listens for GLFW button inputs from the keyboard and reports when they are pressed or
     * released.
     */
    private class KeyboardListener extends GLFWKeyCallback {
        @Override
        public void invoke(long window, int button, int scancode, int action, int mods) {
            if (action == GLFW.GLFW_PRESS) {
                press(button);
            } else if (action == GLFW.GLFW_RELEASE) {
                release(button);
            }
        }
    }

    /**
     * Listens for GLFW button inputs from the mouse and reports when they are pressed or released.
     */
    private class MouseListener extends GLFWMouseButtonCallback {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (action == GLFW.GLFW_PRESS) {
                press(button);
            } else if (action == GLFW.GLFW_RELEASE) {
                release(button);
            }
        }
    }

    /**
     * Attach the buttons to a window.
     *
     * <p>Required to allow button input to be detected.
     *
     * @param window The window to attach to.
     * @param actions The actions that will be activated.
     * @param bindings The button and action bindings.
     */
    void attachToWindow(long window, Actions actions, Bindings bindings) {
        mActions = actions;
        mBindings = bindings;

        // Set the listeners.
        GLFW.glfwSetKeyCallback(window, new KeyboardListener());
        GLFW.glfwSetMouseButtonCallback(window, new MouseListener());
    }

    /**
     * Called when a GLFW button is being pressed.
     *
     * <p>Activate any {@link Action}s associated with that button, according to the mapping in
     * {@link Bindings}.
     *
     * @param button The button being pressed.
     */
    void press(int button) {
        setActivated(button, true);

        for (Action action : mBindings.getActions(button)) {
            mActions.setActivated(action, true);
        }
    }

    /**
     * Called when a GLFW button is released.
     *
     * <p>If there are no other buttons, according to the mapping in {@link Bindings}, activating an
     * {@link Action}, deactivate the action.
     *
     * @param button The button being released.
     */
    void release(int button) {
        setActivated(button, false);

        // Check each action the button triggers, deactivating each action if no other buttons are
        // currently triggering it.
        for (Action action : mBindings.getActions(button)) {
            Boolean deactivate = true;

            for (Integer otherButton : mBindings.getButtons(action)) {
                // If another button that triggers the action is currently activated, do not set
                // action to false.
                if (isActivated(otherButton) == true) {
                    deactivate = false;
                    break;
                }
            }

            // If no other buttons are triggering the action, deactivate the action.
            if (deactivate) {
                mActions.setActivated(action, false);
            }
        }
    }
}
