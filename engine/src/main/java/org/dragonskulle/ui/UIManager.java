/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;

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
     * <p>It will always be the top most element.
     */
    @Getter private Reference<UIRenderable> mHoveredObject;
    /** Global UI appearance */
    @Getter @Setter private UIAppearance mAppearance = new UIAppearance();

    /**
     * Update which UI element is currently hovered by the cursor.
     *
     * @param components a list of currently enabled components
     */
    public void updateHover(Collection<Component> components) {
        mHoveredObject = null;

        Cursor cursor = Actions.getCursor();

        if (cursor == null) {
            return;
        }

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

    /**
     * Get singleton UIManager instance.
     *
     * @return the instance
     */
    public static UIManager getInstance() {
        return SINGLETON;
    }
}
