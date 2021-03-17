/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

/**
 * A simple request handler interface.
 *
 * @author Aurimas Blažulionis
 */
public interface IRequestHandler<T> {
    void handleRequest(T data);
}
