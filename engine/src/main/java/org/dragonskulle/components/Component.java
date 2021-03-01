/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import java.io.Serializable;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

/**
 * Abstract class for a Component
 *
 * @author Harry Stoltz
 *     <p>All components must extend this class, and can also optionally implement any of the
 *     componet interfaces. The destroy method should be overriden to handle the cleanup of any
 *     user-defined variables on a component
 */
public abstract class Component implements Serializable {

    private final Reference<Component> mReference = new Reference<>(this);

    protected GameObject mGameObject;

    private boolean mAwake = false;
    private boolean mEnabled = true;
    private boolean mStarted = false;

    /**
     * Set the destroy flag to true. The component won't actually be destroyed until the end of the
     * current render frame.
     */
    public final void destroy() {
        Engine.getInstance().addDestroyedComponent(this);
    }

    /** Handle the actual destruction of a component. Only called by the engine. */
    public final void engineDestroy() {
        onDestroy();

        mGameObject.removeComponent(this);
        mGameObject = null;
        mReference.clear();
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    protected abstract void onDestroy();

    /**
     * Getter for mGameObject
     *
     * @return mGameObject
     */
    public final GameObject getGameObject() {
        return mGameObject;
    }

    /**
     * Setter for mGameObject
     *
     * @param object New value of mGameObject
     */
    public final void setGameObject(GameObject object) {
        mGameObject = object;
    }

    /**
     * Getter for mAwake
     *
     * @return mAwake
     */
    public final boolean isAwake() {
        return mAwake;
    }

    /**
     * Setter for mAwake
     *
     * @param val New value of mAwake
     */
    public final void setAwake(boolean val) {
        mAwake = val;
    }

    /**
     * Getter for mStarted
     *
     * @return mStarted
     */
    public final boolean isStarted() {
        return mStarted;
    }

    /**
     * Setter for mStarted
     *
     * @param val New value of mStarted
     */
    public final void setStarted(boolean val) {
        mStarted = val;
    }

    /**
     * Getter for mEnabled
     *
     * @return mEnabled
     */
    public final boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Setter for mEnabled
     *
     * @param value New value of mEnabled
     */
    public final void setEnabled(boolean value) {
        mEnabled = value;
    }

    /**
     * Getter for mReference
     *
     * @return mReference
     */
    public final Reference<Component> getReference() {
        return mReference;
    }
}
