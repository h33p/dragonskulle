/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

/**
 * IFixedUpdate interface
 *
 * @author Harry Stoltz
 *     <p>One of the optional interfaces for a component. Contains the single method fixedUpdate.
 */
public interface IFixedUpdate {

    /**
     * Fixed update is aimed to be called a fixed number of times per second. The target rate is
     * defined by UPDATES_PER_SECOND in Engine.java Should be used for things that must be done at a
     * constant rate, such as physics, regardless of render-frame rate
     *
     * @param deltaTime Approximate time between calls to fixedUpdate
     */
    void fixedUpdate(double deltaTime);
}
