/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

/**
 * Possible actions the user can activate.
 *
 * @author Craig Wilbourne
 */
public class Action {

	private String mName;
	
	public Action() {}
	
	public Action(String name) {
		this();
		this.mName = name;
	}
	
	@Override
	public String toString() {
		if(mName == null) {
			return String.format("Action{name:---}", mName);
		}
		return String.format("Action{name:%s}", mName);
	}
	
    /*
	UP,
    DOWN,
    LEFT,
    RIGHT,
    ZOOM_IN,
    ZOOM_OUT,
    SCROLL_UP,
    SCROLL_DOWN,
    DRAG,
    ACTION_1,
    ACTION_2,
    ACTION_3
    */
}
