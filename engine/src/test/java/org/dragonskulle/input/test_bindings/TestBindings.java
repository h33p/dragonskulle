package org.dragonskulle.input.test_bindings;

import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.InputTest;

public class TestBindings extends Bindings {

	public TestBindings() {
		add(InputTest.TEST_KEY_1, TestActions.TEST_ACTION);
		add(InputTest.TEST_KEY_2, TestActions.TEST_ACTION);
		
		submit();
	}
	
}
