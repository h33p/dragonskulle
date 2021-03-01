/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * Once initialised for a specific window, input will listen to all user input and reflect this in
 * the relevant {@link Actions}.
 *
 * @author Craig Wilboure
 */
@Accessors(prefix = "s")
@Log
public class Input {

    /** Allows button presses to trigger actions. */
    private static Buttons sButtons;

    /** Allows bindings between buttons and actions. */
    @Getter private static Bindings sBindings;

    /**
     * Starts the detection of buttons, the cursor and the mouse scroll wheel for a specified
     * window. <b> Automatically submits the current bindings for use.
     *
     * @param window The window Input should listen to, or {@code null} if no GLFW window is to be
     *     connected.
     * @param bindings The button bindings that map buttons to events.
     */
    public static void initialise(Long window, Bindings bindings) {
        // If no bindings are provided, use the basic, default bindings.
        if (bindings == null) {
            bindings = new Bindings();
            log.warning("Custom bindings have not been specified- using basic bindings.");
        }
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
            log.warning("Input is not attatched to a window.");
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
