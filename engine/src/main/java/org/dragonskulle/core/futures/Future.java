/* (C) 2021 DragonSkulle */
package org.dragonskulle.core.futures;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.futures.AwaitFuture.IAwaitFuture;
import org.dragonskulle.core.futures.ProducerFuture.IProducerFuture;
import org.dragonskulle.core.futures.ProducerFuture.IThreadedProducer;
import org.dragonskulle.core.futures.ThenFuture.IThenFuture;

/**
 * Base engine future class.
 *
 * @author Aurimas Blažulionis
 */
@Getter
@Accessors(prefix = "m")
public class Future {
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

    /**
     * Create a future.
     *
     * @param root root of this future chain. {@code null} to set this as root.
     */
    public Future(Future root) {
        mRoot = root == null ? this : root;
    }

    /** Create a future. */
    public Future() {
        this(null);
    }

    /**
     * Invoke a future.
     *
     * <p>This will execute the future. A future implementing this method should schedule an end of
     * loop event that invokes the next future in the chain, unless it's a no-op by itself.
     *
     * @param scene scene context to execute the future in.
     */
    protected void invoke(Scene scene) {
        mComplete = true;
        if (mNextFuture != null) {
            mNextFuture.invoke(scene);
        }
    }

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
     * Schedule the future chain on active scene.
     *
     * <p>This will schedule an event at the end of engine's main loop to process the entire future
     * chain.
     */
    public void schedule() {
        schedule(Scene.getActiveScene());
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
    public AwaitFuture awaitUntil(IAwaitFuture then) {
        AwaitFuture awaiter = new AwaitFuture(mRoot, then);
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
    public AwaitFuture awaitTimeout(float maxSeconds, IAwaitFuture then) {
        Engine instance = Engine.getInstance();

        float endTime = instance.getCurTime() + maxSeconds;

        return awaitUntil(
                (scene) -> {
                    assert (instance.getCurTime() <= endTime);
                    return then.waitCheck(scene);
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

    /**
     * Build a {@link ProducerFuture}.
     *
     * <p>This will build a new {@link ProducerFuture}, and chain it after the current one.
     *
     * <p>This future will produce data on separate thread, and then invoke a post production
     * interface on the main thread.
     *
     * @param <T> type of data to produce.
     * @param dataProducer data to produce.
     * @param postProduction post production action to invoke.
     * @return the newly constructed {@link ProducerFuture}.
     */
    public <T> ProducerFuture<T> threadedProduce(
            IThreadedProducer<T> dataProducer, IProducerFuture<T> postProduction) {
        ProducerFuture<T> producer = new ProducerFuture<>(mRoot, dataProducer, postProduction);
        mNextFuture = producer;
        return producer;
    }
}
