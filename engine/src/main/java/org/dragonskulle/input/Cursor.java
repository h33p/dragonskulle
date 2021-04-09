/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GLFWState;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

/**
 * Once attached to a window, this allows access to:
 *
 * <ul>
 *   <li>The cursor's position in the window scaled to the range [-1, 1], [-1, 1].
 *   <li>The cursor's position, in the window scaled to the range [-1, 1], [-1, 1], at the start of
 *       a drag.
 *   <li>The distance of a drag from the start point.
 *   <li>The scaled angle of a drag from the start point.
 * </ul>
 *
 * @author Craig Wilbourne
 */
@Log
@Accessors(prefix = "m")
public class Cursor {

    /** This cursor's current raw position. */
    private Vector2f mRawPosition = new Vector2f(0, 0);
    /** Scaled mouse cursor position in the range [[-1, 1], [-1, 1]]. */
    private Vector2f mScaledPosition = new Vector2f(0, 0);

    /** The raw starting position of a drag, or {@code null} if no drag is taking place. */
    private Vector2f mRawDragStart;
    /** Scaled mouse drag start position in the range [[-1, 1], [-1, 1]]. */
    private Vector2f mScaledDragStart = new Vector2f(0, 0);

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
                        setPosition((float) x, (float) y);
                        detectDrag();
                    }
                };
        GLFW.glfwSetCursorPosCallback(window, listener);
        GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    /**
     * Set the current position of the mouse.
     *
     * @param x The x position.
     * @param y The y position.
     */
    void setPosition(float x, float y) {
        mRawPosition.set(x, y);
        mScaledPosition = calculateScaled(mRawPosition);
    }

    /**
     * Scale the vector so it is in the range [-1, 1] and [-1, 1], relative to the current window
     * size.
     *
     * @param rawPosition The raw vector coordinates.
     * @return A new vector that has been scaled to the correct range.
     */
    private Vector2f calculateScaled(Vector2f rawPosition) {
        if (rawPosition == null) {
            log.warning("Raw position is null.");
            return null;
        }

        GLFWState state = Engine.getInstance().getGLFWState();
        if (state == null) {
            log.warning("GLFWState is null: may cause unintended side-effects.");
            return null;
        }

        Vector2ic windowSize = state.getWindowSize();
        float scaledX = rawPosition.x() / (float) windowSize.x() * 2f - 1f;
        float scaledY = rawPosition.y() / (float) windowSize.y() * 2f - 1f;

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
        mRawDragStart = new Vector2f(mRawPosition);
        mScaledDragStart = calculateScaled(mRawDragStart);
    }

    /** End a drag in progress. */
    void endDrag() {
        mRawDragStart = null;
    }

    /**
     * Whether the user is currently dragging the cursor.
     *
     * @return {@code true} if the cursor is currently being dragged; otherwise {@code false}.
     */
    public boolean inDrag() {
        return !(mRawDragStart == null);
    }

    /**
     * Get the position the cursor, in the range [-1, 1] and [-1, 1], when the drag began- or {@code
     * null} if there is no drag.
     *
     * @return The initial position of the cursor, relative to the window size, or {@code null} if
     *     no dragging is taking place.
     */
    public Vector2f getDragStart() {
        if (!inDrag()) {
            return null;
        }
        return mScaledDragStart;
    }

    /**
     * Get the angle, in radians, between where this cursor started dragging and its current
     * position.
     *
     * @return The angle, in radians, between the drag start point and current position, or {@code
     *     0} if no dragging is taking place.
     */
    public float getDragAngle() {
        if (!inDrag()) {
            return 0f;
        }
        // Use the raw positions to calculate the correct angle.
        return (float) mRawPosition.angle(mRawDragStart);
    }

    /**
     * Get the distance between where this cursor started dragging and its current position. Will be
     * in the range [0, 2].
     *
     * @return A {@code double} representing the distance from the starting point of the user's
     *     drag, or {@code 0} if no dragging is taking place.
     */
    public float getDragDistance() {
        if (!inDrag()) {
            return 0f;
        }
        return mScaledPosition.distance(mScaledDragStart);
    }

    /**
     * Get the current position of the cursor in the range [-1, 1] and [-1, 1].
     *
     * @return The cursor position, relative to the window size.
     */
    public Vector2fc getPosition() {
        return mScaledPosition;
    }

    /**
     * Get the raw position of the cursor.
     *
     * <p><b>Used only for testing.</b>
     *
     * @return The raw cursor position.
     */
    Vector2f getRawPosition() {
        return mRawPosition;
    }
}
