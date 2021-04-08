/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GLFWState;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2ic;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * Once attached to a window, this allows access to:
 *
 * <ul>
 *   <li>The cursor's current raw position.
 *   <li>The cursor's position scaled to the range [-1, 1], [-1, 1].
 *   <li>The raw starting location of a cursor drag (with the drag being triggered by {@link
 *       Actions#TRIGGER_DRAG}).
 *   <li>The scaled starting location of a cursor drag.
 *   <li>The raw distance of a drag.
 *   <li>The scaled distance of a drag.
 *   <li>The raw angle of a drag.
 *   <li>The scaled angle of a drag.
 * </ul>
 *
 * @author Craig Wilbourne
 */
@Log
@Accessors(prefix = "m")
public class Cursor {

    /** This cursor's current raw position. */
    @Getter private Vector2d mPosition = new Vector2d(0, 0);
    /** Scaled mouse cursor position in the range [[-1, 1], [-1, 1]]. */
    @Getter private Vector2f mScaledPosition = new Vector2f(0, 0);
    
    /** The raw starting position of a drag, or {@code null} if no drag is taking place. */
    @Getter private Vector2d mDragStart;    
    /** Scaled mouse drag start position in the range [[-1, 1], [-1, 1]]. */
    @Getter private Vector2f mScaledDragStart = new Vector2f(0, 0);

    /** Create a new cursor manager. */
    public Cursor() {}

    /**
     * Attach this cursor to a window.
     *
     * <p>Required to allow this cursor to access to this window.
     *
     * @param window The window to attach to.
     */
    void attachToWindow(long window) {
        // Listen for cursor position events.
        GLFWCursorPosCallback listener =
                new GLFWCursorPosCallback() {
                    @Override
                    public void invoke(long window, double x, double y) {
                        setPosition(x, y);
                        detectDrag();
                    }
                };

        GLFW.glfwSetCursorPosCallback(window, listener);
    }

    /**
     * Set the current position of the mouse.
     *
     * @param x The x position.
     * @param y The y position.
     */
    void setPosition(double x, double y) {
        mPosition.set(x, y);
        mScaledPosition = calculateScaled(mPosition);
    }

    private Vector2f calculateScaled(Vector2d rawPosition) {
    	GLFWState state = Engine.getInstance().getGLFWState();
        if(state == null || rawPosition == null) {
        	log.info("GLFWState or raw position is null.");
        	return null;
        }

        Vector2ic windowSize = state.getWindowSize();
        float scaledX = (float) rawPosition.x() / (float) windowSize.x() * 2f - 1f;
        float scaledY = (float) rawPosition.y() / (float) windowSize.y() * 2f - 1f;
        
        return new Vector2f(scaledX, scaledY);
    }
    
    /**
     * Detects whether the cursor is being dragged.
     *
     * <p>Starts a new drag if {@link Action#DRAG} is active. Ends a drag in progress if {@link
     * Action#DRAG} is not active.
     */
    private void detectDrag() {
        if (Actions.TRIGGER_DRAG.isActivated()) {
            if (inDrag() == false) {
                startDrag();
            }
        } else {
            if (inDrag() == true) {
                endDrag();
            }
        }
    }

    /** Start a new drag. */
    void startDrag() {
        mDragStart = new Vector2d(mPosition);
        mScaledDragStart = calculateScaled(mDragStart);
    }

    /** End a drag in progress. */
    void endDrag() {
        mDragStart = null;
    }

    /**
     * Whether the user is currently dragging the cursor.
     *
     * @return {@code true} if the cursor is currently being dragged; otherwise {@code false}.
     */
    public boolean inDrag() {
        return !(mDragStart == null);
    }

    /**
     * Get the direct distance between where this cursor started dragging and its current position.
     *
     * @return A {@code double} representing the distance from the starting point of the user's
     *     drag, or {@code 0} if no dragging is taking place.
     */
    public double getDragDistance() {
        if (!inDrag()) {
            return 0;
        }
        return mPosition.distance(mDragStart);
    }

    /**
     * Get the angle between where this cursor started dragging and its current position.
     *
     * @return The angle, in radians, between the drag start point and current position, or {@code
     *     0} if no dragging is taking place.
     */
    public double getDragAngle() {
        if (!inDrag()) {
            return 0;
        }
        return mPosition.angle(mDragStart);
    }
    
    /**
     * Get the direct distance between where this cursor started dragging and its current position.
     *
     * @return A {@code double} representing the distance from the starting point of the user's
     *     drag, or {@code 0} if no dragging is taking place.
     */
    public double getScaledDragDistance() {
        if (!inDrag()) {
            return 0;
        }
        return mScaledPosition.distance(mScaledDragStart);
    }

    /**
     * Get the angle between where this cursor started dragging and its current position.
     *
     * @return The angle, in radians, between the drag start point and current position, or {@code
     *     0} if no dragging is taking place.
     */
    public double getScaledDragAngle() {
        if (!inDrag()) {
            return 0;
        }
        // DOES NOT WORK
        return mScaledPosition.angle(mScaledDragStart);
    }
}
