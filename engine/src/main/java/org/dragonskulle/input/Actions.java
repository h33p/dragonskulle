package org.dragonskulle.input;

/**
 * Extended to create an easy way to access custom actions.
 * <p>
 * Also contains any {@link Action}s that must always be present (as they are required by internal logic). 
 * 
 * @author Craig Wilbourne
 */
public abstract class Actions {
	//DRAG must always be present, regardless of any other custom actions implemented.
	public final static Action DRAG = new Action("DRAG");
	
	public final static ActionValue<Double> MY_TEST = new ActionValue<Double>("DRAG", 0d);
	
	/**
     * The amount of scrolling done since the last call to {@link Scroll#reset()}.
     * Depending on direction scrolled, this value is negative or positive.
     */
	public final static ActionValue<Double> VALUE_SCROLL = new ActionValue<Double>("VALUE_SCROLL", 0d);
}