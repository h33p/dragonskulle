/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

/**
 * IOnAwake interface.
 *
 * @author Harry Stoltz
 *     <p>One of the optional interfaces for a component. Has the single method onAwake.
 */
public interface IOnAwake {

    /** Called when a component is first added to a scene to allow initial setup of variables. */
    void onAwake();
}
