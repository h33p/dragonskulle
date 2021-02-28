/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Stores {@link Binding}s between buttons and actions.
 *
 * @author Craig Wilbourne
 */
abstract public class Bindings {

    /** Used to log messages. */
    private static final Logger LOGGER = Logger.getLogger("bindings");

    /** Stores all bindings. */
    private final ArrayList<Binding> mBindings = new ArrayList<Binding>();
    
    /** A map between a button and the actions it triggers. */
    private final HashMap<Integer, ArrayList<Action>> mButtonToActions = new HashMap<Integer, ArrayList<Action>>();

    /** A map between an action and the buttons that trigger it. */
    private final HashMap<Action, ArrayList<Integer>> mActionToButtons = new HashMap<Action, ArrayList<Integer>>();
    
    /**
     * Submit the current bindings for use.
     * <p>
     * If any bindings are edited, submit needs to be called again before these changes are reflected. 
     */
    public void submit() {
    	rebind();
    }

    /**
	 * Add a new binding by specifying the button and the actions it triggers.
	 * 
	 * @param button The button code.
	 * @param actions The actions that are triggered by the button.
	 */
	public void add(int button, Action... actions) {
        add(new Binding(button, actions));
    }
    
    /**
     * Add a {@link Binding} to the list of {@link #mBindings}.
     *
     * @param binding The binding to be added.
     */
    void add(Binding binding) {
        mBindings.add(binding);
    }  
    
    /**
     * Get the actions triggered by a specific button.
     * 
     * @param button The button being targeted.
     * @return An {@code ArrayList} of {@link Action}s associated with the button, or an empty
     *     {@code ArrayList}.
     */
    ArrayList<Action> getActions(Integer button) {
        if (!mButtonToActions.containsKey(button)) {
            return new ArrayList<Action>();
        }
        return mButtonToActions.get(button);
    }

    /**
     * Get the buttons that trigger a specific action.
     *
     * @param action The action being targeted.
     * @return An {@code ArrayList} of buttons associated with the {@link Action}, or an empty
     *     {@code ArrayList}.
     */
    ArrayList<Integer> getButtons(Action action) {
        if (!mActionToButtons.containsKey(action)) {
            return new ArrayList<Integer>();
        }
        return mActionToButtons.get(action);
    }  

    /**
     * Allows all of the bindings in {@link #mBindings} to become usable.
     *
     * <p>This temporarily resets {@link #mButtonToActions} and {@link #mActionToButtons}, and then
     * repopulates them with the latest bindings.
     */
    private void rebind() {
        mButtonToActions.clear();
        mActionToButtons.clear();

        for (Binding binding : mBindings) {
            mButtonToActions.put(binding.getButton(), binding.getActions());
        }
        generateActionToButtons();

        LOGGER.info(
                String.format(
                        "Rebinded:\n\tButton to Actions: %s\n\tAction to Buttons: %s",
                        mButtonToActions.toString(), mActionToButtons.toString()));
    }

    /** Use {@link #buttonToAction} to generate the contents of {@link #actionToButton}. */
    private void generateActionToButtons() {
        // Get each button and action combination in mButtonToActions.
        for (Entry<Integer, ArrayList<Action>> entry : mButtonToActions.entrySet()) {
            // For each action, store a list of the buttons that trigger it.
            for (Action action : entry.getValue()) {
                ArrayList<Integer> buttonsList =
                        new ArrayList<
                                Integer>(); // Store a list of buttons that trigger the action.
                buttonsList.add(entry.getKey()); // Add the current button to the list.

                // If the action already has buttons assigned to it, add those buttons to the list.
                if (mActionToButtons.containsKey(action)) {
                    buttonsList.addAll(mActionToButtons.get(action));
                }

                // Store the results.
                mActionToButtons.put(action, buttonsList);
            }
        }
    }
}
