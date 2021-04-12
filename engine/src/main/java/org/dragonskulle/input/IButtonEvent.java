/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

/**
 * Interface for GLTF button press/release events. Used to intercept the events before they activate
 * actions.
 *
 * @author Aurimas Blažulionis
 */
public interface IButtonEvent {
    /**
     * Handle the event
     *
     * @param button the GLTF button code
     */
    void handle(int button);
}
