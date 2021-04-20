/* (C) 2021 DragonSkulle */
package org.dragonskulle.components.lambda;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;

/**
 * Runs FrameUpdate within a Component;
 *
 * @author Oscar L
 */
public class LambdaFrameUpdate extends Component implements IFrameUpdate {
    private final IFrameUpdate mHandler;

    public LambdaFrameUpdate(IFrameUpdate mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
        mHandler.frameUpdate(deltaTime);
    }
}
