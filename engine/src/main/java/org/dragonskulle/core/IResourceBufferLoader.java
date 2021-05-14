/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Load resources from byte buffers.
 *
 * @author Aurimas Blažulionis
 *     <p>This interface is really trivial. A simple lambda that accepts {@code byte[]} and
 *     arguments of {@code ResourceArguments<F>}, and outputs {@code T} implements it automatically!
 */
public interface IResourceBufferLoader<T, F> {
    /**
     * Load resource from byte buffer.
     *
     * @param buffer byte buffer of data.
     * @param args arguments for the loader.
     * @return loaded data.
     * @throws Throwable on any error.
     */
    T loadFromBuffer(byte[] buffer, ResourceArguments<T, F> args) throws Throwable;
}
