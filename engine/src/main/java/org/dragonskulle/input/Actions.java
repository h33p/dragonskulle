/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Used to access all actions and input related values. Extended to add actions (that can be
 * triggered by button input- as defined in {@link Bindings}).
 *
 * <p>By default, it contains the following static values:
 * <li>{@link #DRAG} - An action that is triggered when the cursor is dragged.
 * <li>{@link #sCursor} - Stores everything related to the cursor.
 * <li>{@link #sScroll} - Stores everything related to mouse scrolling.
 *
 *     <p>Example of how to add a new action once extended: <br>
 *     <code> public final static Action <b>NEW_ACTION</b> = new Action("<b>NEW_ACTION_NAME</b>");
 *     </code>
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "s")
public abstract class Actions {
    // DRAG must always be present, regardless of any other custom actions implemented.
    public static final Action DRAG = new Action("DRAG");

    /** Stores everything to do with cursor position and dragging. */
    @Getter private static Cursor sCursor;
    /** Stores everything to do with mouse wheel scrolling. */
    @Getter private static Scroll sScroll;

    /**
     * Refresh select values back to their defaults, ready for their new values.
     *
     * <p>Currently only used for resetting {@link #scroll}.
     */
    public static void refresh() {
        if (sScroll != null) {
            sScroll.reset();
        }
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
