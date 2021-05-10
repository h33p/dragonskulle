/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.input;

import org.dragonskulle.input.Action;
import org.dragonskulle.input.Actions;

/**
 * Defines all actions the game can access.
 *
 * <p>Used to access all user input. <br>
 * e.g. <code> GameActions.UP.isActivated(); </code> <br>
 * e.g. <code> GameActions.getCursor().getDragDistance(); </code>
 *
 * @author Craig Wilbourne
 */
public class GameActions extends Actions {

    public static final Action UP = new Action("UP");
    public static final Action DOWN = new Action("DOWN");
    public static final Action LEFT = new Action("LEFT");
    public static final Action RIGHT = new Action("RIGHT");

    public static final Action ROTATE_LEFT = new Action("ROTATE_LEFT");
    public static final Action ROTATE_RIGHT = new Action("ROTATE_RIGHT");

    public static final Action LEFT_CLICK = new Action("LEFT_CLICK");
    public static final Action RIGHT_CLICK = new Action("RIGHT_CLICK");
    public static final Action MIDDLE_CLICK = new Action("MIDDLE_CLICK");

    public static final Action TOGGLE_PAUSE = new Action("TOGGLE_PAUSE");

    public static final Action ATTACK_MODE = new Action("ATTACK_MODE");
    public static final Action SELL_MODE = new Action("SELL_MODE");
    public static final Action BUILD_MODE = new Action("BUILD_MODE");
}
