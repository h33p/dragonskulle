package org.dragonskulle.input;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

class KeyListener {

	public static final Logger LOGGER = Logger.getLogger("KeyListener");
	
	private StorageHandler storageHandler;
	private ButtonHandler buttonHandler;
	private ActionHandler actionHandler;
	
	public KeyListener(long window, StorageHandler storageHandler, ButtonHandler buttonHandler, ActionHandler actionHandler) {
		this.storageHandler = storageHandler;
		this.buttonHandler = buttonHandler;
		this.actionHandler = actionHandler;
		
		GLFW.glfwSetKeyCallback(window, create());
	}

	private GLFWKeyCallback create() {
		return new GLFWKeyCallback() {
			
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				//LOGGER.info(String.format("key: %d\nscancode: %d\naction: %d\nmods: %d", key, scancode, action, mods));
				
				if(action == GLFW.GLFW_PRESS) {
					keyDown(key);					
				} else if(action == GLFW.GLFW_RELEASE) {
					keyUp(key);
				}
				
				//LOGGER.info(actions.toString());
			}
		};
	}
	
	private void keyDown(Integer key) {
		buttonHandler.setActivated(key, true);
		
		if(storageHandler.getButtonToAction().containsKey(key)) {
			ArrayList<Action> selectedActions = storageHandler.getButtonToAction().get(key);
			
			for (Action selectedAction : selectedActions) {
				actionHandler.setActivated(selectedAction, true);
			}						
		}
	}
	
	private void keyUp(Integer key) {
		buttonHandler.setActivated(key, false);
		
		if(storageHandler.getButtonToAction().containsKey(key)) {
			ArrayList<Action> selectedActions = storageHandler.getButtonToAction().get(key);
			
			for (Action selectedAction : selectedActions) {
				ArrayList<Integer> buttonsCauseAction = storageHandler.getActionToButton().get(selectedAction);
				if(buttonsCauseAction == null) {
					LOGGER.severe("buttonsCauseAction is null!");
					return;
				}
				
				Boolean remove = true;
				for(Integer selectedButton : buttonsCauseAction) {
					if(buttonHandler.contains(selectedButton) && buttonHandler.isActivated(selectedButton) == true) {
						// Another button is currently activated, so do not set action to false.
						remove = false;
						break;
					}
				}
				
				if(remove) {
					actionHandler.setActivated(selectedAction, false);
				}
			}					
		}
	}
	
}
