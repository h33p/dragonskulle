/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.GameObject.IBuildHandler;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.joml.Vector4fc;

/**
 * Can be used when constructing vertical UIs. Each contains the {@link IUIBuildHandler} to use and
 * a relative xOffset.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
public class BuildHandlerInfo extends Component implements IUIBuildHandler {
    @Override
    public void handleUIBuild(GameObject go) {
        TransformUI transformUI = go.getTransform(TransformUI.class);
        Vector4fc parentAnchor = transformUI.getParentAnchor();
        transformUI.setParentAnchor(
                parentAnchor.x() + mXOffset, parentAnchor.y(), parentAnchor.z(), parentAnchor.w());
        mChild.handleUIBuild(go);
    }

    /** The {@link IBuildHandler} used to build a UI element. */
    private final IUIBuildHandler mChild;
    /** The xOffset, relative to its normal x position in the vertical UI. */
    private final float mXOffset;

    /**
     * Store a {@link IUIBuildHandler} and its offset in comparison to the rest of the UI.
     *
     * @param handler The build handler.
     * @param xOffset The offset.
     */
    public BuildHandlerInfo(IUIBuildHandler handler, float xOffset) {
        mChild = handler;
        mXOffset = xOffset;
    }

    @Override
    protected void onDestroy() {}
}
