/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.util.Collection;
import java.util.List;
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
    @Setter
    @Accessors(prefix = "s")
    public static CursorType sNextCursor = UIAppearance.getDefaultCursor();

    private static CursorType sCurrentCursor = null;

    public static final UIManager SINGLETON = new UIManager();

    /**
     * Reference to currently hovered UI element.
     *
     * <p>It will always be the top most element.
     */
    @Getter private Reference<UIRenderable> mHoveredObject;
    /** Global UI appearance. */
    @Getter @Setter private UIAppearance mAppearance = new UIAppearance();

    /**
     * Update which UI element is currently hovered by the cursor.
     *
     * @param components a list of currently enabled components
     */
    public void uiUpdate(Collection<Component> components) {
        mHoveredObject = null;

        Cursor cursor = Actions.getCursor();

        if (cursor == null) {
            return;
        }

        if (sCurrentCursor != sNextCursor) {
            sCurrentCursor = sNextCursor;
            Cursor.setCursor(sCurrentCursor);
        }
        setNextCursor(UIAppearance.getDefaultCursor());

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
     * Builds elements in a x width grid.
     *
     * @param go object to build the children on.
     * @param width the number of elements to be displayed in one row
     * @param startX starting X parent anchor, this will be consistent for all elements.
     * @param startY starting Y parent anchor, this will act as an offset.
     * @param endY ending Y parent anchor, this will be consistent for all elements.
     * @param ySeperation the offset between rows
     * @param elems list of buildable UI elements. Can be UITextRect elements, lambdas, custom
     *     objects, or a mix of them.
     */
    public void buildGridUI(
            GameObject go,
            int width,
            float startX,
            float startY,
            float endY,
            float ySeperation,
            List<IUIBuildHandler> elems) {
        float offsetY = 0f;
        while (elems.iterator().hasNext()) {
            IUIBuildHandler[] row = new IUIBuildHandler[width];
            for (int i = 0; i < width; i++) {
                if (elems.size() > 0) {
                    row[i] = elems.remove(0);
                } else {
                    row[i] = null;
                }
            }

            buildHorizontalUI(go, startX, startY + offsetY, endY + offsetY, row);
            offsetY += ySeperation;
        }
    }

    /** UI build handler. */
    public static interface IUIBuildHandler {
        /**
         * Handle building of UI object.
         *
         * <p>This method will be called to allow initial setup of the object. It will be already
         * linked up with its parent, if there is any.
         *
         * @param go The {@link GameObject}.
         */
        void handleUIBuild(GameObject go);
    }

    /**
     * Buildable UI component.
     *
     * <p>This class implements default UI build handler, which just adds itself as component on the
     * object.
     */
    public abstract static class UIBuildableComponent extends Component implements IUIBuildHandler {
        @Override
        public void handleUIBuild(GameObject go) {
            go.addComponent(this);
        }
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
    public void buildVerticalUi(
            GameObject go, float startY, float startX, float endX, IUIBuildHandler... elems) {
        buildVerticalUi(go, startY, startX, endX, 0.5f, elems);
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
     * @param pivotX X pivot point, used for alienment.
     * @param elems list of buildable UI elements. Can be UITextRect elements, lambdas, custom
     *     objects, or a mix of them.
     */
    public void buildVerticalUi(
            GameObject go,
            float startY,
            float startX,
            float endX,
            float pivotX,
            IUIBuildHandler... elems) {
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
                            transform.setPivotOffset(pivotX, 0.5f);
                            handler.handleUIBuild(child);
                        });
            }

            cnt++;
        }
    }

    /**
     * Build a horizontal UI on the object
     *
     * <p>This method will build UI elements horizontally in accordance to {@link UIAppearance}
     * settings.
     *
     * @param go object to build the children on.
     * @param startX starting X parent anchor, this will act as an offset.
     * @param startY starting Y parent anchor, this will be consistent for all elements.
     * @param endY ending Y parent anchor, this will be consistent for all elements.
     * @param elems list of buildable UI elements. Can be UITextRect elements, lambdas, custom
     *     objects, or a mix of them.
     */
    public void buildHorizontalUI(
            GameObject go, float startX, float startY, float endY, IUIBuildHandler... elems) {
        int cnt = 0;

        for (IUIBuildHandler handler : elems) {
            final float curX =
                    cnt * (mAppearance.getHorizontalUIElemWidth() + mAppearance.getHorizUIElemGap())
                            + startX;

            if (handler != null) {
                go.buildChild(
                        "ui_child",
                        new TransformUI(true),
                        (child) -> {
                            TransformUI transform = child.getTransform(TransformUI.class);
                            transform.setParentAnchor(curX, startY, curX, endY);
                            transform.setMargin(0, 0, mAppearance.getHorizontalUIElemWidth(), 0);
                            handler.handleUIBuild(child);
                        });
            }

            cnt++;
        }
    }

    /**
     * Build child UI elements at specified anchor offsets.
     *
     * @param go game object to build the children on.
     * @param initialX initial X starting anchor.
     * @param initialY initial Y starting anchor.
     * @param width consistent anchor based width to keep.
     * @param height consistent anchor based height to keep.
     * @param offsetX how much each element will be offset from one another on X axis.
     * @param offsetY how much each element will be offset from one another on Y axis.
     * @param elems A list of {@link IUIBuildHandler}s.
     */
    public void buildWithAnchorOffset(
            GameObject go,
            float initialX,
            float initialY,
            float width,
            float height,
            float offsetX,
            float offsetY,
            IUIBuildHandler... elems) {
        int cnt = 0;

        for (IUIBuildHandler handler : elems) {
            final float curX = initialX + offsetX * cnt;
            final float curY = initialY + offsetY * cnt;
            final float endX = curX + width;
            final float endY = curY + height;

            if (handler != null) {
                go.buildChild(
                        "ui_child",
                        new TransformUI(true),
                        (child) -> {
                            TransformUI transform = child.getTransform(TransformUI.class);
                            transform.setParentAnchor(curX, curY, endX, endY);
                            handler.handleUIBuild(child);
                        });
            }

            cnt++;
        }
    }

    /**
     * Build child UI elements to the right of the object.
     *
     * <p>This method will use anchors to place {@code elems} almost immediately after this object
     * on the right.
     *
     * @param go object to build children on.
     * @param elems objects to build.
     */
    public void buildRightOf(GameObject go, IUIBuildHandler... elems) {
        buildWithAnchorOffset(go, 1f, 0f, 1f, 1f, 1f + mAppearance.getHorizUIElemGap(), 0f, elems);
    }

    /**
     * Builds an object with some children right of it.
     *
     * @param first the main object to build.
     * @param elems the elements that will be built right of {@code first}.
     * @return wrapped build handler that will build first on the object, and elems as children.
     */
    public IUIBuildHandler buildWithChildrenRightOf(
            IUIBuildHandler first, IUIBuildHandler... elems) {
        return (go) -> {
            first.handleUIBuild(go);
            buildRightOf(go, elems);
        };
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
