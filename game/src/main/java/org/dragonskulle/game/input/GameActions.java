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

    public static final Action ZOOM_IN = new Action("ZOOM_IN");
    public static final Action ZOOM_OUT = new Action("ZOOM_OUT");

    public static final Action MENU_UP = new Action("MENU_UP");
    public static final Action MENU_DOWN = new Action("MENU_DOWN");

    public static final Action ACTION_1 = new Action("ACTION_1");
    public static final Action ACTION_2 = new Action("ACTION_2");
    public static final Action ACTION_3 = new Action("ACTION_3");
}
