package org.dragonskulle.input.test_bindings;

import org.dragonskulle.input.Action;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.InputTest;

public class TestActions extends Actions {
	/** An action that is triggered by {@link InputTest#TEST_KEY_1} and {@link InputTest#TEST_KEY_2}. */
	public final static Action TEST_ACTION = new Action("TEST_ACTION");
	
	/** An action that is not currently linked to any buttons. */
	public final static Action TEST_ACTION_2 = new Action("TEST_ACTION_2");
	
	/** An action that is not linked to any buttons. */
	public final static Action UNLINKED_ACTION = new Action("UNLINKED_ACTION"); 
}
