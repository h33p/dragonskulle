/* (C) 2021 DragonSkulle */
package org.dragonskulle.core.futures;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.futures.AwaitFuture.IAwaitFuture;
import org.dragonskulle.core.futures.ThenFuture.IThenFuture;

/**
 * Base engine future class.
 *
 * @author Aurimas Blažulionis
 */
@Getter
@Accessors(prefix = "m")
public abstract class Future {
    /**
     * Root of this chain.
     *
     * <p>Calling {@link schedule} will schedule execution of this future, which then will
     * eventually propagate control to the current future, and the futures chained afterwards.
     */
    private final Future mRoot;
    /**
     * Future that comes after this one.
     *
     * <p>{@link invoke} will perform a self action, and once the control flow is supposed to move
     * forwards, the next future will be queued for the next iteration of main loop.
     */
    protected Future mNextFuture;

    /** Stores whether the future is completed or not. */
    protected boolean mComplete;

    public Future(Future root) {
        mRoot = root == null ? this : root;
    }

    protected abstract void invoke(Scene scene);

    /**
     * Schedule the future chain on a given scene.
     *
     * <p>This will schedule an event at the end of engine's main loop to process the entire future
     * chain.
     *
     * @param scene scene context to execute the future in.
     */
    public void schedule(Scene scene) {
        Engine.getInstance().scheduleEndOfLoopEvent(() -> mRoot.invoke(scene));
    }

    /**
     * Build a {@link ThenFuture}.
     *
     * <p>This build a new {@link ThenFuture}, and chain it after the current one.
     *
     * @param then functional interface that will be executed by returned future.
     * @return the newly constructed {@link ThenFuture}.
     */
    public ThenFuture then(IThenFuture then) {
        ThenFuture doer = new ThenFuture(mRoot, then);
        mNextFuture = doer;
        return doer;
    }

    /**
     * Build a {@link AwaitFuture}.
     *
     * <p>This build a new {@link AwaitFuture}, and chain it after the current one.
     *
     * <p>This future will wait until a condition yields true.
     *
     * @param then functional interface that will be executed repeatedly by returned future.
     * @return the newly constructed {@link AwaitFuture}.
     */
    public AwaitFuture awaitUntil(IAwaitFuture awaitFn) {
        AwaitFuture awaiter = new AwaitFuture(mRoot, awaitFn);
        mNextFuture = awaiter;
        return awaiter;
    }

    /**
     * Build a {@link AwaitFuture}.
     *
     * <p>This build a new {@link AwaitFuture}, and chain it after the current one.
     *
     * <p>This future will wait until a condition yields true, or timeout is reached.
     *
     * @param maxSeconds maximum timeout for the future, before it fails with assert.
     * @param then functional interface that will be executed repeatedly by returned future, until
     *     it succeeds, or timeout is reached.
     * @return the newly constructed {@link AwaitFuture}.
     */
    public AwaitFuture awaitTimeout(float maxSeconds, IAwaitFuture awaitFn) {
        Engine instance = Engine.getInstance();

        float endTime = instance.getCurTime() + maxSeconds;

        return awaitUntil(
                (scene) -> {
                    assert (instance.getCurTime() <= endTime);
                    return awaitFn.waitCheck(scene);
                });
    }

    /**
     * Build a {@link AwaitFuture}.
     *
     * <p>This build a new {@link AwaitFuture}, and chain it after the current one.
     *
     * <p>This future will wait until input future gets completed, synchronizing control flow.
     *
     * @param future another future to synchronize against.
     * @return the newly constructed {@link AwaitFuture}.
     */
    public AwaitFuture syncWith(Future future) {
        return awaitUntil((scene) -> future.isComplete());
    }
}
