/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

/**
 * ILateFrameUpdate interface
 *
 * @author Harry Stoltz
 *     <p>One of the optional interfaces for a component. Has the single method lateFrameUpdate.
 */
public interface ILateFrameUpdate {

    /**
     * Late frame update is called every single render frame, after any calls to fixed update. It is
     * the final update call before rendering is performed.
     *
     * @param deltaTime Approximate time since last call to lateFrameUpdate
     */
    void lateFrameUpdate(float deltaTime);
}
