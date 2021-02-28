package org.dragonskulle.input;

/**
 * Extended to create an easy way to access custom actions.
 * <p>
 * Also contains any {@link Action}s that must always be present (as they are required by internal logic). 
 * 
 * @author Craig Wilbourne
 */
public class Actions {
	//DRAG must always be present, regardless of any other custom actions implemented.
	public final static Action DRAG = new Action("DRAG");
}