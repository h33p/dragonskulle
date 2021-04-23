/* (C) 2021 DragonSkulle */
package org.dragonskulle.components.lambda;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;

/**
 * Run things onStart as a Component.
 *
 * @author Oscar L
 */
@Log
public class LambdaOnStart extends Component implements IOnStart {
    private final IOnStart mHandler;

    /**
     * Constructor.
     *
     * @param handler the handler
     */
    public LambdaOnStart(IOnStart handler) {
        mHandler = handler;
    }

    @Override
    public void onStart() {
        mHandler.onStart();
    }

    @Override
    protected void onDestroy() {}
}
