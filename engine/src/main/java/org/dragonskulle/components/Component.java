/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.dragonskulle.core.GameObject;

/**
 * Abstract class for a Component
 *
 * @author Harry Stoltz
 *      <p>
 *          All components must extend this class, and can also optionally implement any of the
 *          componet interfaces.
 *          The destroy method should be overriden to handle the cleanup of any user-defined
 *          variables on a component
 *      </p>
 */
public abstract class Component {

    private GameObject mGameObject;
    private boolean mEnabled;

    /**
     * Destroy a component, override to handle destroying of specific components
     */
    public void destroy() {
        mGameObject = null;
    }

    /**
     * Getter for mGameObject
     * @return mGameObject
     */
    public GameObject getGameObject() { return mGameObject; }

    /**
     * Setter for mGameObject
     * @param object New value of mGameObject
     */
    public void setGameObject(GameObject object) { mGameObject = object; }

    /**
     * Getter for mEnabled
     * @return mEnabled
     */
    public boolean getEnabled() { return mEnabled; }

    /**
     * Setter for mEnabled
     * @param value New value of mEnabled
     */
    public void setEnabled(boolean value) { mEnabled = value; }
}