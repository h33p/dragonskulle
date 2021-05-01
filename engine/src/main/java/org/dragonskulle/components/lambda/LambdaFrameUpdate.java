/* (C) 2021 DragonSkulle */
package org.dragonskulle.components.lambda;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;

/**
 * Runs things on FrameUpdate as a lambda.
 *
 * @author Oscar L
 */
public class LambdaFrameUpdate extends Component implements IFrameUpdate {
    private final IFrameUpdate mHandler;
    /**
     * Constructor.
     *
     * @param handler the handler to be ran on frame update
     */
    public LambdaFrameUpdate(IFrameUpdate handler) {
        this.mHandler = handler;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
        mHandler.frameUpdate(deltaTime);
    }
}
