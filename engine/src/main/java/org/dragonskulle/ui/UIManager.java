/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.GameObject;
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
     * Update which UI element is currently hovered by the cursor
     *
     * @param components a list of currently enabled components
     */
    public void updateHover(Collection<Component> components) {
        mHoveredObject = null;

        Cursor cursor = Actions.getCursor();

        if (cursor == null) return;

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

    public static interface IUIBuildHandler {
        /**
         * Handle building of UI object
         *
         * <p>This method will be called to allow initial setup of the object. It will be already
         * linked up with its parent, if there is any.
         */
        void handleUIBuild(GameObject go);
    }

    /**
     * Build a vertical UI on the object
     *
     * <p>This method will build UI elements vertically in accordance to {@link UIAppearance}
     * settings.
     *
     * @param go object to build the children on.
     * @param startY starting Y parent anchor, this will act as an offset.
     * @param startX starting X parent anchor, this will be consistent for all elements.
     * @param endX ending X parent anchor, this will be consistent for all elements.
     * @param elems list of buildable UI elements. Can be UITextRect elements, lambdas, custom
     *     objects, or a mix of them.
     */
    public void buildVerticalUI(
            GameObject go, float startY, float startX, float endX, IUIBuildHandler... elems) {
        int cnt = 0;

        for (IUIBuildHandler handler : elems) {
            final float curY =
                    cnt
                                    * (mAppearance.getVerticalUIElemHeight()
                                            + mAppearance.getVerticalUIElemGap())
                            + startY;

            if (handler != null) {
                go.buildChild(
                        "ui_child",
                        new TransformUI(true),
                        (child) -> {
                            TransformUI transform = child.getTransform(TransformUI.class);
                            transform.setParentAnchor(startX, curY, endX, curY);
                            transform.setMargin(0, 0, 0, mAppearance.getVerticalUIElemHeight());
                            handler.handleUIBuild(child);
                        });
            }

            cnt++;
        }
    }

    /** Get singleton UIManager instance */
    public static UIManager getInstance() {
        return SINGLETON;
    }
}
