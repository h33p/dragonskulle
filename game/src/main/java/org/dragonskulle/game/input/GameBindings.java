/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.input;

import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Scroll;
import org.dragonskulle.ui.UIButton;
import org.lwjgl.glfw.GLFW;

/**
 * Binds buttons to {@link GameActions}.
 *
 * @author Craig Wilbourne
 */
public class GameBindings extends Bindings {

    /** Create all initial game bindings. */
    public GameBindings() {
        addBinding(GLFW.GLFW_KEY_UP, GameActions.UP, GameActions.MENU_UP);
        addBinding(GLFW.GLFW_KEY_W, GameActions.UP, GameActions.MENU_UP);

        addBinding(GLFW.GLFW_KEY_DOWN, GameActions.DOWN, GameActions.MENU_DOWN);
        addBinding(GLFW.GLFW_KEY_S, GameActions.DOWN, GameActions.MENU_DOWN);

        addBinding(GLFW.GLFW_KEY_LEFT, GameActions.LEFT);
        addBinding(GLFW.GLFW_KEY_A, GameActions.LEFT);

        addBinding(GLFW.GLFW_KEY_RIGHT, GameActions.RIGHT);
        addBinding(GLFW.GLFW_KEY_D, GameActions.RIGHT);
        
        addBinding(GLFW.GLFW_MOUSE_BUTTON_LEFT, GameActions.LEFT_CLICK, GameActions.TRIGGER_DRAG, UIButton.UI_PRESS);
        addBinding(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GameActions.RIGHT_CLICK);
        addBinding(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GameActions.ACTION_3);

        addBinding(Scroll.UP, GameActions.MENU_UP, GameActions.ZOOM_IN);
        addBinding(Scroll.DOWN, GameActions.MENU_DOWN, GameActions.ZOOM_OUT);
    }
}
