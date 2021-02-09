/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Load resources from byte buffers
 *
 * @author Aurimas Blažulionis
 *     <p>This interface is really trivial. A simple lambda that accepts {@code byte[]}, and outputs
 *     {@code T} implements it automatically!
 */
public interface IResourceLoader<T> {
    T loadFromBuffer(byte[] buffer);
}
