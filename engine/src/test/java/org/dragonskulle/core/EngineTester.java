/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Allows to test engine/game code.
 *
 * @author Aurimas Blažulionis
 */
public class EngineTester {
    @Getter
    @Accessors(prefix = "m")
    public abstract static class EngineFuture {
        private final EngineFuture mRoot;
        protected EngineFuture mNextFuture;
        protected boolean mComplete;

        public EngineFuture(EngineFuture root) {
            mRoot = root == null ? this : root;
        }

        public abstract void invoke(Scene scene);

        public void execute(Scene scene) {
            Engine.getInstance().scheduleEndOfLoopEvent(() -> mRoot.invoke(scene));
        }

        public EngineDoer then(IEngineDo event) {
            EngineDoer doer = new EngineDoer(mRoot, event);
            mNextFuture = doer;
            return doer;
        }

        public EngineAwaiter awaitUntil(IEngineAwait awaitFn) {
            EngineAwaiter awaiter = new EngineAwaiter(mRoot, awaitFn);
            mNextFuture = awaiter;
            return awaiter;
        }

        public EngineAwaiter awaitTimeout(float maxSeconds, IEngineAwait awaitFn) {
            Engine instance = Engine.getInstance();

            float endTime = instance.getCurTime() + maxSeconds;

            return awaitUntil(
                    (scene) -> {
                        assert (instance.getCurTime() <= endTime);
                        return awaitFn.waitCheck(scene);
                    });
        }

        public EngineAwaiter syncWith(EngineFuture future) {
            return awaitUntil((scene) -> future.isComplete());
        }
    }

    public static class EngineDoer extends EngineFuture {
        final IEngineDo mDo;

        public EngineDoer(EngineFuture root, IEngineDo doFn) {
            super(root);
            mDo = doFn;
        }

        public EngineDoer(IEngineDo event) {
            this(null, event);
        }

        @Override
        public void invoke(Scene scene) {
            mDo.invoke(scene);
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

    public static class EngineAwaiter extends EngineFuture {
        final IEngineAwait mAwait;

        public EngineAwaiter(EngineFuture root, IEngineAwait awaitFn) {
            super(root);
            mAwait = awaitFn;
        }

        public EngineAwaiter(IEngineAwait awaitFn) {
            this(null, awaitFn);
        }

        @Override
        public void invoke(Scene scene) {
            if (!mAwait.waitCheck(scene)) {
                Engine.getInstance().scheduleEndOfLoopEvent(() -> invoke(scene));
            } else if (mNextFuture != null) {
                mComplete = true;
                mNextFuture.invoke(scene);
            }
        }
    }

    public static interface IEngineDo {
        void invoke(Scene scene);
    }

    public static interface IEngineAwait {
        boolean waitCheck(Scene scene);
    }

    public static void testEngine(EngineFuture... futures) {

        Engine engine = Engine.getInstance();

        int cnt = 0;
        int[] loadedScenes = {0};

        for (EngineFuture future : futures) {
            Scene scene = new Scene("test" + cnt);
            future.then(engine::unloadScene).then((__) -> loadedScenes[0]--).execute(scene);
            cnt++;
            loadedScenes[0]++;
            engine.activateScene(scene);
        }

        engine.startFixedDebug(() -> loadedScenes[0] != 0);
    }
}
