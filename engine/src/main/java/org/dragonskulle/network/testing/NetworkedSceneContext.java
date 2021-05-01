/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.testing;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.core.futures.AwaitFuture.IAwaitFuture;
import org.dragonskulle.core.futures.Future;
import org.dragonskulle.core.futures.ThenFuture;
import org.dragonskulle.core.futures.ThenFuture.IThenFuture;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkManager.ISceneBuilder;

@Getter
@Accessors(prefix = "m")
public class NetworkedSceneContext {
    private NetworkManager mManager;
    Future mFuture;

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

    public NetworkedSceneContext then(IThenFuture doFn) {
        mFuture = mFuture.then(doFn);
        return this;
    }

    public NetworkedSceneContext awaitUntil(IAwaitFuture awaitFn) {
        mFuture = mFuture.awaitUntil(awaitFn);
        return this;
    }

    public NetworkedSceneContext awaitTimeout(float maxSeconds, IAwaitFuture awaitFn) {
        mFuture = mFuture.awaitTimeout(maxSeconds, awaitFn);
        return this;
    }

    public NetworkedSceneContext syncWith(Future future) {
        mFuture = mFuture.syncWith(future);
        return this;
    }

    public NetworkedSceneContext syncWith(NetworkedSceneContext ctx) {
        return syncWith(ctx.mFuture);
    }
}
