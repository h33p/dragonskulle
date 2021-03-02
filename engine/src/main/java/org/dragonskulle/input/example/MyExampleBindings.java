/* (C) 2021 DragonSkulle */
package org.dragonskulle.input.example;

import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Scroll;
import org.lwjgl.glfw.GLFW;

public class MyExampleBindings extends Bindings {

    public MyExampleBindings() {
        addBinding(GLFW.GLFW_KEY_Q, MyExampleActions.BONUS);

        addBinding(GLFW.GLFW_KEY_UP, MyExampleActions.UP, MyExampleActions.MENU_UP);
        addBinding(GLFW.GLFW_KEY_W, MyExampleActions.UP, MyExampleActions.MENU_UP);

        addBinding(GLFW.GLFW_KEY_DOWN, MyExampleActions.DOWN, MyExampleActions.MENU_DOWN);
        addBinding(GLFW.GLFW_KEY_S, MyExampleActions.DOWN, MyExampleActions.MENU_DOWN);

        addBinding(GLFW.GLFW_KEY_LEFT, MyExampleActions.LEFT);
        addBinding(GLFW.GLFW_KEY_A, MyExampleActions.LEFT);

        addBinding(GLFW.GLFW_KEY_RIGHT, MyExampleActions.RIGHT);
        addBinding(GLFW.GLFW_KEY_D, MyExampleActions.RIGHT);

        addBinding(GLFW.GLFW_MOUSE_BUTTON_LEFT, MyExampleActions.ACTION_1, MyExampleActions.TRIGGER_DRAG);
        addBinding(GLFW.GLFW_MOUSE_BUTTON_RIGHT, MyExampleActions.ACTION_2);
        addBinding(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, MyExampleActions.ACTION_3);

        addBinding(Scroll.UP, MyExampleActions.MENU_UP, MyExampleActions.ZOOM_IN);
        addBinding(Scroll.DOWN, MyExampleActions.MENU_DOWN, MyExampleActions.ZOOM_OUT);
    }
}
