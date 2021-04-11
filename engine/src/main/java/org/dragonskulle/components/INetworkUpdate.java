/* (C) 2021 DragonSkulle */

package org.dragonskulle.components;

/**
 * INetworkUpdate interface.
 *
 * @author Aurimas Blažulionis
 * @author Harry Stoltz
 *
 *     <p>One of the optional interfaces for a component. Contains the single method networkUpdate.
 */
public interface INetworkUpdate {
    /**
     * Network update is called before fixed updates upto UPDATES_PER_SECOND times. When this is
     * called all network references should be set up on the components.
     */
    void networkUpdate();
}
