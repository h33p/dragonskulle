/* (C) 2021 DragonSkulle */
package org.dragonskulle.input.example;

import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Scroll;
import org.lwjgl.glfw.GLFW;

public class MyExampleBindings extends Bindings {

    public MyExampleBindings() {
        add(GLFW.GLFW_KEY_Q, MyExampleActions.BONUS);

        add(GLFW.GLFW_KEY_UP, MyExampleActions.UP, MyExampleActions.MENU_UP);
        add(GLFW.GLFW_KEY_W, MyExampleActions.UP, MyExampleActions.MENU_UP);

        add(GLFW.GLFW_KEY_DOWN, MyExampleActions.DOWN, MyExampleActions.MENU_DOWN);
        add(GLFW.GLFW_KEY_S, MyExampleActions.DOWN, MyExampleActions.MENU_DOWN);

        add(GLFW.GLFW_KEY_LEFT, MyExampleActions.LEFT);
        add(GLFW.GLFW_KEY_A, MyExampleActions.LEFT);

        add(GLFW.GLFW_KEY_RIGHT, MyExampleActions.RIGHT);
        add(GLFW.GLFW_KEY_D, MyExampleActions.RIGHT);

        add(GLFW.GLFW_MOUSE_BUTTON_LEFT, MyExampleActions.ACTION_1, MyExampleActions.DRAG);
        add(GLFW.GLFW_MOUSE_BUTTON_RIGHT, MyExampleActions.ACTION_2);
        add(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, MyExampleActions.ACTION_3);

        add(Scroll.UP, MyExampleActions.MENU_UP, MyExampleActions.ZOOM_IN);
        add(Scroll.DOWN, MyExampleActions.MENU_DOWN, MyExampleActions.ZOOM_OUT);

        submit();
    }
}
