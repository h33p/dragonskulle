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
	
	/** Stores everything to do with mouse wheel scrolling. */
	public static Scroll scroll;
	/** Stores everything to do with cursor position and dragging. */
	public static Cursor cursor;
	
	/**
	 * Refresh select values back to their defaults, ready for their new values. 
     * <p>
     * Currently only used for resetting {@link #scroll}.
	 */
	public static void refresh() {
		if(scroll != null) {
			scroll.reset();
		}
	}
}