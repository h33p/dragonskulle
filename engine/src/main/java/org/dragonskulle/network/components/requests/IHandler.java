/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

/**
 * A simple request handler interface.
 *
 * @author Aurimas Blažulionis
 */
public interface IHandler<T> {
    /**
     * Handle the request.
     *
     * @param data data of the request.
     */
    void invokeHandler(T data);
}
