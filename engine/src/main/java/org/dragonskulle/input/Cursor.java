/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GLFWState;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWImage;

/**
 * Once attached to a window, this allows access to do the following.
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
    static {
        ResourceManager.registerResource(
                BufferedImage.class,
                (args) -> String.format("textures/ui/%s.png", args.getName()),
                (buffer, __) -> ImageIO.read(new ByteArrayInputStream(buffer)));
    }

    private static final float DRAGGED_THRESHOLD = 0.02f;

    /**
     * This cursor's current raw position.
     */
    private Vector2f mRawPosition = new Vector2f(0, 0);
    /**
     * Scaled mouse cursor position in the range [[-1, 1], [-1, 1]].
     */
    private Vector2f mScaledPosition = new Vector2f(0, 0);

    /**
     * The raw starting position of a drag, or {@code null} if no drag is taking place.
     */
    private Vector2f mRawDragStart;
    /**
     * Scaled mouse drag start position in the range [[-1, 1], [-1, 1]].
     */
    private Vector2f mScaledDragStart = new Vector2f(0, 0);
    /**
     * Maximum amount the cursor was dragged from the starting position
     */
    private float mMaxDragDistance = 0f;

    /**
     * Create a new cursor manager.
     */
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

        try {
            setCustomCursor(window);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set the cursor on a window
    }

    /**
     * Sets a custom hardware cursor.
     *
     * @param window the window to attach to
     * @throws IOException thrown if the cursor file doesn't exist
     */
    private void setCustomCursor(long window) throws IOException {
        Resource<BufferedImage> fcursor = ResourceManager.getResource(BufferedImage.class, "cursor");
        BufferedImage bImage = fcursor.get();
        Image scaledImage =
                bImage.getScaledInstance(
                        bImage.getWidth() / 2, bImage.getHeight() / 2, Image.SCALE_FAST);

        // from https://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
        bImage =
                new BufferedImage(
                        scaledImage.getWidth(null),
                        scaledImage.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D bGr = bImage.createGraphics();
        bGr.drawImage(scaledImage, 0, 0, null);
        bGr.dispose();
        // end
        int width = bImage.getWidth();
        int height = bImage.getHeight();

        int[] pixels = new int[width * height];
        bImage.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        MathUtils.intARGBtoByteRGBA(pixels, height, width, buffer);
        GLFWImage image = GLFWImage.create();
        image.set(width, height, buffer);
        long cursor = GLFW.glfwCreateCursor(image, 10, 10);

        GLFW.glfwSetCursor(window, cursor);
    }

    /**
     * Set the current position of the mouse.
     *
     * @param x The x position.
     * @param y The y position.
     */
    void setPosition(float x, float y) {
        mRawPosition.set(x, y);
        calculateScaled(mRawPosition, mScaledPosition);
        mMaxDragDistance = Math.max(mMaxDragDistance, getDragDistance());
    }

    /**
     * Scale the vector so it is in the range [-1, 1] and [-1, 1], relative to the current window
     * size.
     *
     * @param rawPosition    The raw vector coordinates.
     * @param scaledPosition The vector where the result will be written to.
     */
    private void calculateScaled(Vector2fc rawPosition, Vector2f scaledPosition) {
        if (rawPosition == null) {
            log.warning("Raw position is null.");
            return;
        }

        GLFWState state = Engine.getInstance().getGLFWState();
        if (state == null) {
            log.warning("GLFWState is null: may cause unintended side-effects.");
            return;
        }

        Vector2ic windowSize = state.getWindowSize();
        float scaledX = rawPosition.x() / (float) windowSize.x() * 2f - 1f;
        float scaledY = rawPosition.y() / (float) windowSize.y() * 2f - 1f;

        scaledPosition.set(scaledX, scaledY);
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

    /**
     * Start a new drag.
     */
    void startDrag() {
        mRawDragStart = new Vector2f(mRawPosition);
        calculateScaled(mRawDragStart, mScaledDragStart);
        mMaxDragDistance = 0;
    }

    /**
     * End a drag in progress.
     */
    void endDrag() {
        mRawDragStart = null;
        mMaxDragDistance = 0;
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
     * Whether the cursor was dragged a little amount
     *
     * @return {@code true} if the cursor was dragged a little amount
     */
    public boolean hadLittleDrag() {
        return mMaxDragDistance >= DRAGGED_THRESHOLD;
    }

    /**
     * Get the position the cursor, in the range [-1, 1] and [-1, 1], when the drag began- or {@code
     * null} if there is no drag.
     *
     * @return The initial position of the cursor, relative to the window size, or {@code null} if
     * no dragging is taking place.
     */
    public Vector2fc getDragStart() {
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
     * 0} if no dragging is taking place.
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
     * drag, or {@code 0} if no dragging is taking place.
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
    Vector2fc getRawPosition() {
        return mRawPosition;
    }
}
