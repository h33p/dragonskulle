/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.GameObject.IBuildHandler;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;

/**
 * Can be used when constructing vertical UIs. Each contains the {@link IUIBuildHandler} to use and
 * a relative xOffset.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
public class BuildHandlerInfo {
    /** The {@link IBuildHandler} used to build a UI element. */
    @Getter private IUIBuildHandler mHandler;
    /** The xOffset, relative to its normal x position in the vertical UI. */
    @Getter private float mXOffset;

    /**
     * Store a {@link IUIBuildHandler}.
     *
     * @param handler The build handler.
     */
    public BuildHandlerInfo(IUIBuildHandler handler) {
        mHandler = handler;
    }

    /**
     * Store a {@link IUIBuildHandler} and its offset in comparison to the rest of the UI.
     *
     * @param handler The build handler.
     * @param xOffset The offset.
     */
    public BuildHandlerInfo(IUIBuildHandler handler, float xOffset) {
        this(handler);
        mXOffset = xOffset;
    }
}
