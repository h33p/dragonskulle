package org.dragonskulle.input.test_bindings;

import org.dragonskulle.input.BindingsTemplate;
import org.dragonskulle.input.InputTest;

public class TestBindings extends BindingsTemplate {

	public TestBindings() {
		add(InputTest.TEST_KEY_1, TestActions.TEST_ACTION);
		add(InputTest.TEST_KEY_2, TestActions.TEST_ACTION);
	}
	
}
