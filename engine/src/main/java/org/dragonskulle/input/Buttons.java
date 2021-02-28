/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.HashMap;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

/**
 * Once attached to a window, this allows for buttons to trigger actions, as defined by the mapping
 * in {@link Bindings}.
 *
 * @author Craig Wilbourne
 */
class Buttons {

    /** Allows the mapping between buttons and actions to be accessed. */
    private Bindings mBindings;
    
    private final HashMap<Integer, Boolean> buttons = new HashMap<Integer, Boolean>();

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
     * Create a new buttons manager.
     *
     * @param bindings The bindings of actions to buttons.
     */
    public Buttons(Bindings bindings) {
        mBindings = bindings;
    }

    /**
     * Attach the buttons to a window.
     *
     * <p>Required to allow button input to be detected.
     *
     * @param window The window to attach to.
     */
    void attachToWindow(long window) {
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
        setPressed(button, true);

        for (Action action : mBindings.getActions(button)) {
        	action.setActivated(true);
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
    	setPressed(button, false);
        
        // Check each action the button triggers, deactivating the action if no other buttons are currently triggering it.
        for (Action action : mBindings.getActions(button)) {
            attemptDeactivate(action);
        }
        
    }
    
    /**
     * If all buttons for an action have been released, deactivate the action.
     * 
     * @param action The action to be deactivated, if possible.
     */
    private void attemptDeactivate(Action action) {
        for (Integer button : mBindings.getButtons(action)) {
            if (isPressed(button) == true) {
                // A button is still triggering the action- so do not deactivate the action.
            	return;
            }
        }
        
        // All buttons that trigger the action have been released- so deactivate the action.
        action.setActivated(false);
    }
    
    /**
     * Record whether a specific button is being pressed or not.
     * 
     * @param button The button. 
     * @param pressed Whether the button is being pressed.
     */
    void setPressed(int button, boolean pressed){
    	buttons.put(button, pressed);
    }
    
    /**
     * Whether a specified button is currently being pressed.
     * 
     * @param button The button.
     * @return Whether the button is currently being pressed.
     */
    boolean isPressed(int button) {
        Boolean value = buttons.get(button);
        if(value == null) {
        	return false;
        }
    	return value.booleanValue();
    }
    
}
