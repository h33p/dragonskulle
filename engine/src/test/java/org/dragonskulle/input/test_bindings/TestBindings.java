/* (C) 2021 DragonSkulle */
package org.dragonskulle.input.test_bindings;

import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.InputTest;

public class TestBindings extends Bindings {

    public TestBindings() {
    	addBinding(InputTest.TEST_KEY_1, TestActions.TEST_ACTION);
    	addBinding(InputTest.TEST_KEY_2, TestActions.TEST_ACTION);
    }
}
