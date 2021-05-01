/* (C) 2021 DragonSkulle */
package org.dragonskulle.components.lambda;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;

/**
 * Run things on FixedUpdate as a Lambda.
 *
 * @author Oscar L
 */
public class LambdaFixedUpdate extends Component implements IFixedUpdate {
    private final IFixedUpdate mHandler;

    /**
     * Constructor.
     *
     * @param handler the handler to be ran on fixed update
     */
    public LambdaFixedUpdate(IFixedUpdate handler) {
        mHandler = handler;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        mHandler.fixedUpdate(deltaTime);
    }
}
