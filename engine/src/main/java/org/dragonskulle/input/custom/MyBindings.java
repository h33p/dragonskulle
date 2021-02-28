package org.dragonskulle.input.custom;

import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Scroll;
import org.lwjgl.glfw.GLFW;

public class MyBindings extends Bindings {

	public MyBindings() {
		add(GLFW.GLFW_KEY_Q, MyActions.BONUS);
        
        add(GLFW.GLFW_KEY_UP, MyActions.UP, MyActions.MENU_UP);
        add(GLFW.GLFW_KEY_W, MyActions.UP, MyActions.MENU_UP);

        add(GLFW.GLFW_KEY_DOWN, MyActions.DOWN, MyActions.MENU_DOWN);
        add(GLFW.GLFW_KEY_S, MyActions.DOWN, MyActions.MENU_DOWN);

        add(GLFW.GLFW_KEY_LEFT, MyActions.LEFT);
        add(GLFW.GLFW_KEY_A, MyActions.LEFT);

        add(GLFW.GLFW_KEY_RIGHT, MyActions.RIGHT);
        add(GLFW.GLFW_KEY_D, MyActions.RIGHT);

        add(GLFW.GLFW_MOUSE_BUTTON_LEFT, MyActions.ACTION_1, MyActions.DRAG);
        add(GLFW.GLFW_MOUSE_BUTTON_RIGHT, MyActions.ACTION_2);
        add(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, MyActions.ACTION_3);

        add(Scroll.UP, MyActions.MENU_UP, MyActions.ZOOM_IN);
        add(Scroll.DOWN, MyActions.MENU_DOWN, MyActions.ZOOM_OUT);
        
        submit();
	}
	
}
