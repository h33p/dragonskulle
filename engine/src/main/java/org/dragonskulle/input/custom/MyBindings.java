package org.dragonskulle.input.custom;

import org.dragonskulle.input.CustomBindings;
import org.dragonskulle.input.Scroll;
import org.lwjgl.glfw.GLFW;

public class MyBindings extends CustomBindings {

	public MyBindings() {
        add(GLFW.GLFW_KEY_Q, MyActions.BONUS);
        
        add(GLFW.GLFW_KEY_UP, MyActions.UP, MyActions.SCROLL_UP);
        add(GLFW.GLFW_KEY_W, MyActions.UP, MyActions.SCROLL_UP);

        add(GLFW.GLFW_KEY_DOWN, MyActions.DOWN, MyActions.SCROLL_DOWN);
        add(GLFW.GLFW_KEY_S, MyActions.DOWN, MyActions.SCROLL_DOWN);

        add(GLFW.GLFW_KEY_LEFT, MyActions.LEFT);
        add(GLFW.GLFW_KEY_A, MyActions.LEFT);

        add(GLFW.GLFW_KEY_RIGHT, MyActions.RIGHT);
        add(GLFW.GLFW_KEY_D, MyActions.RIGHT);

        add(GLFW.GLFW_MOUSE_BUTTON_LEFT, MyActions.ACTION_1, MyActions.DRAG);
        add(GLFW.GLFW_MOUSE_BUTTON_RIGHT, MyActions.ACTION_2);
        add(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, MyActions.ACTION_3);

        add(Scroll.UP, MyActions.SCROLL_UP, MyActions.ZOOM_IN);
        add(Scroll.DOWN, MyActions.SCROLL_DOWN, MyActions.ZOOM_OUT);
	}
	
}
