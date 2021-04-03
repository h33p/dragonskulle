/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * Used to access all actions and input related values. Extended to add actions (that can be
 * triggered by button input- as defined in {@link Bindings}).
 *
 * <p>By default, it contains the following static values:
 *
 * <ul>
 *   <li>{@link #TRIGGER_DRAG} - An action that is triggered when the cursor is dragged.
 *   <li>{@link #sCursor} - Stores everything related to the cursor.
 *   <li>{@link #sScroll} - Stores everything related to mouse scrolling.
 * </ul>
 *
 * <p>Example of how to add a new action once extended: <br>
 * <code> public final static Action <b>NEW_ACTION</b> = new Action("<b>NEW_ACTION_NAME</b>");
 *     </code>
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "s")
@Log
public abstract class Actions {
    /**
     * When actiavted, TRIGGER_DRAG will cause {@link #sCursor} to start detecting cursor movement
     * as a drag.
     *
     * <p>TRIGGER_DRAG must always be present, regardless of any other custom actions implemented.
     */
    public static final Action TRIGGER_DRAG = new Action("DRAG");

    /** Stores everything to do with cursor position and dragging. */
    @Getter private static Cursor sCursor;
    /** Stores everything to do with mouse wheel scrolling. */
    @Getter private static Scroll sScroll;

    /** A list of {@link Action}s that have been activated this frame. */
    private static ArrayList<Action> mJustActivated = new ArrayList<Action>();
    
    /**
     * Refresh select values back to their defaults, ready for their new values.
     *
     * <p>Currently only used for resetting {@link #sScroll}.
     */
    static void refresh() {
        if (sScroll != null) {
            sScroll.reset();
        }
        
        // The frame has been complete, so the actions have no longer just been activated.
        for (Action action : mJustActivated) {
			action.setJustActivated(false);
		}
        mJustActivated.clear();
    }

    /**
     * Add an {@link Action} to the list of actions that have been activated this frame.
     * @param action The action that has been activated this frame.
     */
    static void addJustActivated(Action action) {
    	mJustActivated.add(action);
    }
    
    /**
     * Set the cursor.
     *
     * @param cursor The cursor.
     */
    static void setCursor(Cursor cursor) {
        sCursor = cursor;
    }

    /**
     * Set the scroll.
     *
     * @param scroll The scroll.
     */
    static void setScroll(Scroll scroll) {
        sScroll = scroll;
    }
}
