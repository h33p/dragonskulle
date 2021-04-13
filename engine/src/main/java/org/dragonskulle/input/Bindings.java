/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Stores {@link Binding}s between buttons and actions.
 *
 * @author Craig Wilbourne
 */
public class Bindings {

    /** A map between a button and its binding. */
    private final HashMap<Integer, Binding> mButtonToBinding = new HashMap<Integer, Binding>();

    /** A map between an action and the buttons that trigger it. */
    private final HashMap<Action, ArrayList<Integer>> mActionToButtons =
            new HashMap<Action, ArrayList<Integer>>();

    /**
     * Add a new binding by specifying the button and the actions it triggers. If a {@link Binding}
     * already exists for the button, add to the pre-existing binding.
     *
     * @param button The button code.
     * @param actions The actions that are triggered by the button.
     */
    public void addBinding(int button, Action... actions) {
        Binding binding = mButtonToBinding.get(button);
        // If a binding already exists, simply add to it.
        if (binding != null) {
            binding.addActions(actions);
            return;
        }

        // Add a new binding for the button.
        Binding newBinding = new Binding(button, actions);
        mButtonToBinding.put(button, newBinding);

        // Populate mActionToButtons.
        for (Action action : newBinding.getActions()) {
            // Get the buttons that trigger the action.
            ArrayList<Integer> buttons = mActionToButtons.get(action);
            if (buttons == null) {
                // No buttons for this action have been stored yet- so create a new entry.
                ArrayList<Integer> newButtons = new ArrayList<Integer>();
                newButtons.add(button);
                mActionToButtons.put(action, newButtons);
                continue;
            }
            // Add the button.
            buttons.add(button);
        }
    }

    /**
     * Remove the stored binding for a specific button.
     *
     * @param button The button.
     */
    public void removeBinding(int button) {
        Binding binding = mButtonToBinding.get(button);
        if (binding == null) {
            return;
        }

        // Remove references to the button in mActionToButtons.
        for (Action action : binding.getActions()) {
            ArrayList<Integer> buttons = mActionToButtons.get(action);
            if (buttons != null) {
                buttons.remove(Integer.valueOf(button));
                // If there are no buttons related to an action, remove the action.
                if (buttons.size() == 0) {
                    mActionToButtons.remove(action);
                }
            }
        }

        // Remove the binding.
        mButtonToBinding.remove(button);
    }

    /**
     * Get the actions triggered by a specific button.
     *
     * @param button The button being targeted.
     * @return An {@code ArrayList} of {@link Action}s associated with the button, or an empty
     *     {@code ArrayList}.
     */
    ArrayList<Action> getActions(Integer button) {
        Binding binding = mButtonToBinding.get(button);
        if (binding == null) {
            return new ArrayList<Action>();
        }
        return binding.getActions();
    }

    /**
     * Get all actions that have bindings.
     *
     * @return A {@code Collection} of {@link Action}s that have buttons bound to them.
     */
    Collection<Action> getActions() {
        return mActionToButtons.keySet();
    }

    /**
     * Get the buttons that trigger a specific action.
     *
     * @param action The action being targeted.
     * @return An {@code ArrayList} of buttons associated with the {@link Action}, or an empty
     *     {@code ArrayList}.
     */
    ArrayList<Integer> getButtons(Action action) {
        ArrayList<Integer> buttons = mActionToButtons.get(action);
        if (buttons == null) {
            return new ArrayList<Integer>();
        }
        return buttons;
    }
}
