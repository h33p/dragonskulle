/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

/**
 * Custom exception thrown by the renderer.
 *
 * @author Aurimas Blažulionis
 */
public class RendererException extends Exception {
    /**
     * Create a new renderer exception.
     *
     * @param msg message to attach.
     */
    RendererException(String msg) {
        super(msg);
    }
}
