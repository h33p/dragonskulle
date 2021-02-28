package org.dragonskulle.input.test_bindings;

import org.dragonskulle.input.CustomBindings;
import org.dragonskulle.input.InputTest;

public class TestBindings extends CustomBindings {

	public TestBindings() {
		add(InputTest.TEST_KEY_1, TestActions.TEST_ACTION);
		add(InputTest.TEST_KEY_2, TestActions.TEST_ACTION);
	}
	
}
