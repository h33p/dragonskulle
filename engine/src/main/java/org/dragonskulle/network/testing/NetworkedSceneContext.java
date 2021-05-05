/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.testing;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.core.futures.AwaitFuture;
import org.dragonskulle.core.futures.AwaitFuture.IAwaitFuture;
import org.dragonskulle.core.futures.Future;
import org.dragonskulle.core.futures.ThenFuture;
import org.dragonskulle.core.futures.ThenFuture.IThenFuture;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkManager.ISceneBuilder;

/**
 * Context for a single networked scene.
 *
 * @author Aurimas Blažulionis
 *     <p>This class provides a way to chain futures that will then be called within networked game
 *     context.
 */
@Getter
@Accessors(prefix = "m")
public class NetworkedSceneContext {
    private NetworkManager mManager;
    Future mFuture;

    /**
     * Constructor for {@link NetworkedSceneContext}.
     *
     * @param templates networked templates to use in the context
     * @param builder game scene builder
     */
    public NetworkedSceneContext(TemplateManager templates, ISceneBuilder builder) {
        mManager = new NetworkManager(templates, builder);

        mFuture =
                new ThenFuture(
                        (scene) -> {
                            scene.addRootObject(
                                    new GameObject(
                                            "netman", handle -> handle.addComponent(mManager)));
                        });
    }

    /**
     * Chain a {@link ThenFuture} on the context.
     *
     * @param doFn then function to schedule.
     * @return {@code this}
     */
    public NetworkedSceneContext then(IThenFuture doFn) {
        mFuture = mFuture.then(doFn);
        return this;
    }

    /**
     * Chain an await {@link AwaitFuture} on the context.
     *
     * @param awaitFn await function to schedule.
     * @return {@code this}
     */
    public NetworkedSceneContext awaitUntil(IAwaitFuture awaitFn) {
        mFuture = mFuture.awaitUntil(awaitFn);
        return this;
    }

    /**
     * Chain a timeout await {@link AwaitFuture} on the context.
     *
     * @param maxSeconds maximum time to wait before assertion fails.
     * @param awaitFn await function to schedule.
     * @return {@code this}
     */
    public NetworkedSceneContext awaitTimeout(float maxSeconds, IAwaitFuture awaitFn) {
        mFuture = mFuture.awaitTimeout(maxSeconds, awaitFn);
        return this;
    }

    /**
     * Chain a sync await {@link AwaitFuture} on the context.
     *
     * @param future future to sync with
     * @return {@code this}
     */
    public NetworkedSceneContext syncWith(Future future) {
        mFuture = mFuture.syncWith(future);
        return this;
    }

    /**
     * Chain a sync await {@link AwaitFuture} on the context.
     *
     * @param ctx context to synchronize with
     * @return {@code this}
     */
    public NetworkedSceneContext syncWith(NetworkedSceneContext ctx) {
        return syncWith(ctx.mFuture);
    }
}
