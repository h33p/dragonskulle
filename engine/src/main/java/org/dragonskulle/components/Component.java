/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

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

    private final Reference<Component> mReference = new Reference<>(this);

    private GameObject mGameObject;

    private boolean mAwake = false;
    private boolean mEnabled;
    private boolean mStarted = false;

    // TODO: boolean for tracking destroy

    /**
     * Base implementation of destroy, cannot be overridden but calls the overridable method
     */
    public final void destroy() {
        mGameObject = null;
        mReference.clear();

        onDestroy();
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    private void onDestroy() {}

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
     * Getter for mAwake
     *
     * @return mAwake
     */
    public boolean isAwake() { return mAwake; }

    /**
     * Setter for mAwake
     *
     * @param val New value of mAwake
     */
    public void setAwake(boolean val) { mAwake = val; }

    /**
     * Getter for mStarted
     *
     * @return mStarted
     */
    public boolean isStarted() { return mStarted; }

    /**
     * Setter for mStarted
     *
     * @param val New value of mStarted
     */
    public void setStarted(boolean val) { mStarted = val; }

    /**
     * Getter for mEnabled
     * @return mEnabled
     */
    public boolean isEnabled() { return mEnabled; }

    /**
     * Setter for mEnabled
     * @param value New value of mEnabled
     */
    public void setEnabled(boolean value) { mEnabled = value; }
}