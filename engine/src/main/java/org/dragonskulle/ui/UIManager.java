/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.util.Collection;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GLFWState;
import org.dragonskulle.core.Reference;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2ic;

/**
 * General UI manager.
 *
 * <p>Currently, this manager goes through all objects, and finds which object UI element was
 * hovered.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIManager {
    public static final UIManager SINGLETON = new UIManager();

    /**
     * Reference to currently hovered UI element.
     *
     * <p>It will always be the top most button.
     */
    @Getter private Reference<UIRenderable> mHoveredObject;

    /** Scaled mouse cursor coordinates in [[-1, 1], [-1, 1]] range */
    @Getter private Vector2f mScaledCursorCoords = new Vector2f();

    /**
     * Update which UI element is currently hovered by the cursor
     *
     * @param components a list of currently enabled components
     */
    public void updateHover(Collection<Component> components) {
        mHoveredObject = null;

        Cursor cursor = Actions.getCursor();

        if (cursor == null) return;

        Vector2d cursorCoords = cursor.getPosition();

        GLFWState state = Engine.getInstance().getGLFWState();

        Vector2ic windowSize = state.getWindowSize();

        mScaledCursorCoords.set(
                (float) cursorCoords.x() / (float) windowSize.x() * 2f - 1f,
                (float) cursorCoords.y() / (float) windowSize.y() * 2f - 1f);

        int curDepth = 0;

        for (Component component : components) {
            if (component instanceof UIRenderable) {
                UIRenderable rend = (UIRenderable) component;
                int depth = rend.getGameObject().getDepth();
                if (rend.cursorOver() && depth >= curDepth) {
                    mHoveredObject = rend.getReference(UIRenderable.class);
                    curDepth = depth;
                }
            }
        }
    }

    /** Get singleton UIManager instance */
    public static UIManager getInstance() {
        return SINGLETON;
    }
}
