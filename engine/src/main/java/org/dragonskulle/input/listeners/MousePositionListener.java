package org.dragonskulle.input.listeners;

import java.util.logging.Logger;

import org.dragonskulle.input.Action;
import org.dragonskulle.input.storage.Actions;
import org.dragonskulle.input.storage.MousePosition;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MousePositionListener {
	
	public static final Logger LOGGER = Logger.getLogger("MousePositionListener");
	
	private MousePosition mousePosition;
	private Actions actions;
	
	public MousePositionListener(long window, MousePosition mousePosition, Actions actions) {
		this.mousePosition = mousePosition;
		this.actions = actions;
		
		GLFW.glfwSetCursorPosCallback(window, create());
	}
	
	private GLFWCursorPosCallback create() {
		
		return new GLFWCursorPosCallback() {
			
			@Override
			public void invoke(long window, double x, double y) {
				//LOGGER.info(String.format("x: %f\ny: %f", x, y));
				mousePosition.setPosition(x, y);
				
				if(actions.isActivated(Action.MOUSE_LEFT) || actions.isActivated(Action.MOUSE_RIGHT) || actions.isActivated(Action.MOUSE_MIDDLE)) {
					if(mousePosition.isDragInProgress() == false) {
						mousePosition.startDrag(x, y);
					}
				} else {
					if(mousePosition.isDragInProgress() == true) {
						mousePosition.endDrag(x, y);
					}
				}
			}
		};
	}	
	
}
