/* (C) 2021 DragonSkulle */
package org.dragonskulle.core.futures;

import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Scene;

/**
 * "Then" future.
 *
 * @author Aurimas Blažulionis
 *     <p>This future can be built with {@link Future#then}. It simply invoke the input body, and
 *     schedule the next future to be executed for the next iteration of main loop, if there is one.
 */
public class ThenFuture extends Future {
    /** Interface describing a queued action to be performed. */
    public static interface IThenFuture {
        /**
         * This will perform a single action appropriate for a future.
         *
         * @param scene scene from which the future is executed.
         */
        void invoke(Scene scene);
    }

    /** The underlying action to invoke. */
    private final IThenFuture mThen;

    /**
     * Constructor for {@link ThenFuture}.
     *
     * @param root root of the future chain.
     * @param then action to invoke.
     */
    public ThenFuture(Future root, IThenFuture then) {
        super(root);
        mThen = then;
    }

    /**
     * Constructor for {@link ThenFuture}.
     *
     * <p>This future will be constructed as root future.
     *
     * @param then action to invoke.
     */
    public ThenFuture(IThenFuture then) {
        this(null, then);
    }

    @Override
    protected void invoke(Scene scene) {
        mThen.invoke(scene);
        mComplete = true;
        if (mNextFuture != null) {
            Engine.getInstance()
                    .scheduleEndOfLoopEvent(
                            () -> {
                                mNextFuture.invoke(scene);
                            });
        }
    }
}
