/* (C) 2021 DragonSkulle */
package org.dragonskulle.core.futures;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Scene;

/**
 * "Producer" future.
 *
 * @author Aurimas Blažulionis
 *     <p>This future can be built with {@link Future#threadedProduce}. It will invoke a producer in
 *     separate thread, and wait until the thread finishes. Once it happens, future handler is
 *     called, and the next future is called in the chain
 */
@Accessors(prefix = "m")
public class ProducerFuture<T> extends Future {

    /** Interface describing a threaded data production action. */
    public static interface IThreadedProducer<T> {
        /**
         * This will be invoked on a new thread inside {@link ProducerFuture}, which will then await
         * for this method to return produced data.
         *
         * @return produced value.
         */
        T produce();
    }

    /** Interface describing a queued action to be performed. */
    public static interface IProducerFuture<T> {
        /**
         * This will perform a single action appropriate for a future, after data is produced.
         *
         * @param scene scene from which the future is executed.
         * @param data data produced inside the producer.
         */
        void invoke(Scene scene, T data);
    }

    /** The underlying action to invoke after producing the result. */
    private final IProducerFuture<T> mPostProduction;
    /** Thread used in production. */
    private final Thread mThread;

    /** Produced data of this future. It becomes valid, only when {@code mComplete} is true. */
    @Getter private T mProducedData;

    /**
     * Constructor for {@link ProducerFuture}.
     *
     * @param root root of the future chain.
     * @param producer data to produce.
     * @param postProduction post production action to invoke.
     */
    public ProducerFuture(
            Future root, IThreadedProducer<T> producer, IProducerFuture<T> postProduction) {
        super(root);
        mPostProduction = postProduction;
        mThread = new Thread(() -> mProducedData = producer.produce());
    }

    /**
     * Constructor for {@link ProducerFuture}.
     *
     * <p>This future will be constructed as root future.
     *
     * @param producer data to produce.
     * @param postProduction post production action to invoke.
     */
    public ProducerFuture(IThreadedProducer<T> producer, IProducerFuture<T> postProduction) {
        this(null, producer, postProduction);
    }

    @Override
    protected void invoke(Scene scene) {
        mThread.start();
        forwardCheck(scene);
    }

    /**
     * This method will check if production was finished, then invoke the post production interface,
     * alongside scheduling the next future.
     *
     * @param scene scene of the context.
     */
    private void forwardCheck(Scene scene) {
        if (mThread.isAlive()) {
            Engine.getInstance().scheduleEndOfLoopEvent(() -> forwardCheck(scene));
            return;
        }

        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mComplete = true;

        mPostProduction.invoke(scene, mProducedData);

        if (mNextFuture != null) {
            Engine.getInstance()
                    .scheduleEndOfLoopEvent(
                            () -> {
                                mNextFuture.invoke(scene);
                            });
        }
    }
}
