/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

/**
 * IOnStart interface
 *
 * @author Harry Stoltz
 *     <p>One of the optional interfaces for a component. Has the single method onStart.
 */
public interface IOnStart {

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    void onStart();
}
