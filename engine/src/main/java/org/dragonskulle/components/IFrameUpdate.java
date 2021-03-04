/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

/**
 * IFrameUpdate interface
 *
 * @author Harry Stoltz
 *     <p>One of the optional interfaces for a component. Has the single method frameUpdate.
 */
public interface IFrameUpdate {

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    void frameUpdate(float deltaTime);
}
