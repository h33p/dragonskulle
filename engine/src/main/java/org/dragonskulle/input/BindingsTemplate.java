package org.dragonskulle.input;

import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Stores {@link Binding}s between buttons and actions.
 * 
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
abstract public class BindingsTemplate {

	@Getter
	private final ArrayList<Binding> mBindings = new ArrayList<Binding>();

	/**
	 * Add a binding to the custom bindings by specifying the button and the actions it triggers.
	 * 
	 * @param button The button.
	 * @param actions The actions that are triggered by the button.
	 */
	public void add(int button, Action... actions) {
        add(new Binding(button, actions));
    }
	
	/**
     * Add a {@link Binding} to the custom bindings.
     *
     * @param binding The binding to be added.
     */
	public void add(Binding binding) {
        mBindings.add(binding);
    }
	
}
