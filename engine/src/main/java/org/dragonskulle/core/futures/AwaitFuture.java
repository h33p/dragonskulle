/* (C) 2021 DragonSkulle */
package org.dragonskulle.core.futures;

import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Scene;

/**
 * "Await" future.
 *
 * @author Aurimas Blažulionis
 *     <p>This future executes an input condition interface repeatedly until it returns {@code
 *     true}.
 *     <p>There are several ways to build an {@link AwaitFuture}. The base layer is {@link
 *     Future#awaitUntil(IAwaitFuture)}, which will call the function raw. Another way is {@link
 *     Future#awaitTimeout(float, IAwaitFuture)}, which will assertion fail, if execution fails.
 *     Preferably only use in tests. Third way is {@link Future#syncWith(Future)}, which is meant to
 *     wait until another input future completes. This is useful to sync client-server communication
 *     in tests.
 *     <p>After completion, this future will schedule the next future for execution.
 */
public class AwaitFuture extends Future {

    /** Interface describing an await check. */
    public static interface IAwaitFuture {
        /**
         * Perform the completion check.
         *
         * @param scene scene context of the future.
         * @return {@code true}, if the await future should complete. {@code false otherwise}.
         */
        boolean waitCheck(Scene scene);
    }

    /** Await check condition that will be executed. */
    private final IAwaitFuture mAwait;

    /**
     * Constructor for {@link AwaitFuture}.
     *
     * @param root root of the future chain.
     * @param await condition to check for.
     */
    public AwaitFuture(Future root, IAwaitFuture await) {
        super(root);
        mAwait = await;
    }

    /**
     * Constructor for {@link AwaitFuture}.
     *
     * <p>This future will be constructed as root future.
     *
     * @param await condition to check for.
     */
    public AwaitFuture(IAwaitFuture await) {
        this(null, await);
    }

    @Override
    protected void invoke(Scene scene) {
        if (!mAwait.waitCheck(scene)) {
            Engine.getInstance().scheduleEndOfLoopEvent(() -> invoke(scene));
        } else if (mNextFuture != null) {
            mComplete = true;
            mNextFuture.invoke(scene);
        }
    }
}
