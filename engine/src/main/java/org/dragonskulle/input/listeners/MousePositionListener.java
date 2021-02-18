package org.dragonskulle.input.listeners;

import java.util.logging.Logger;

import org.dragonskulle.input.storage.Position;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MousePositionListener {
	
	public static final Logger LOGGER = Logger.getLogger("MousePositionListener");
	
	private Position position;
	
	public MousePositionListener(long window, Position position) {
		this.position = position;
		
		GLFW.glfwSetCursorPosCallback(window, create());
	}
	
	private GLFWCursorPosCallback create() {
		
		return new GLFWCursorPosCallback() {
			
			@Override
			public void invoke(long window, double xpos, double ypos) {
				//LOGGER.info(String.format("xpos: %f\nypos: %f", xpos, ypos));
				position.setX(xpos);
				position.setY(ypos);
			}
		};
	}	
	
}
