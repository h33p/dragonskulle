/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.input;

import org.dragonskulle.devtools.RenderDebug;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Scroll;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIInputBox;
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

        addBinding(GLFW.GLFW_KEY_LEFT, GameActions.LEFT, UIInputBox.CURSOR_LEFT);
        addBinding(GLFW.GLFW_KEY_A, GameActions.LEFT);

        addBinding(GLFW.GLFW_KEY_RIGHT, GameActions.RIGHT, UIInputBox.CURSOR_RIGHT);
        addBinding(GLFW.GLFW_KEY_D, GameActions.RIGHT);

        addBinding(GLFW.GLFW_KEY_Q, GameActions.ROTATE_LEFT);

        addBinding(GLFW.GLFW_KEY_E, GameActions.ROTATE_RIGHT);

        addBinding(GLFW.GLFW_KEY_HOME, UIInputBox.CURSOR_START);
        addBinding(GLFW.GLFW_KEY_END, UIInputBox.CURSOR_END);

        addBinding(GLFW.GLFW_KEY_ESCAPE, UIInputBox.INPUT_FINISH);

        addBinding(GLFW.GLFW_KEY_DELETE, UIInputBox.INPUT_DELETE);
        addBinding(GLFW.GLFW_KEY_BACKSPACE, UIInputBox.INPUT_BACKSPACE);

        addBinding(
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                GameActions.LEFT_CLICK,
                GameActions.TRIGGER_DRAG,
                UIButton.UI_PRESS);
        addBinding(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GameActions.RIGHT_CLICK);
        addBinding(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GameActions.ACTION_3);

        addBinding(Scroll.UP, GameActions.MENU_UP, GameActions.ZOOM_IN);
        addBinding(Scroll.DOWN, GameActions.MENU_DOWN, GameActions.ZOOM_OUT);

        addBinding(GLFW.GLFW_KEY_F3, RenderDebug.DEBUG_ACTION);
    }
}
