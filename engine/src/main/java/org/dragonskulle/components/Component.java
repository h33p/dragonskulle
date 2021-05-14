/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

/**
 * Abstract class for a Component.
 *
 * @author Harry Stoltz
 *     <p>All components must extend this class, and can also optionally implement any of the
 *     componet interfaces. The destroy method should be overriden to handle the cleanup of any
 *     user-defined variables on a component
 */
@Accessors(prefix = "m")
public abstract class Component {

    @Getter private Reference<Component> mReference = new Reference<>(this);

    @Getter @Setter protected GameObject mGameObject;

    @Getter @Setter private boolean mAwake = false;
    @Getter private boolean mEnabled = true;
    @Getter @Setter private boolean mStarted = false;

    /**
     * Set the destroy flag to true. The component won't actually be destroyed until the end of the
     * current render frame.
     */
    public final void destroy() {
        Engine.getInstance().addDestroyedComponent(this);
    }

    /** Handle the actual destruction of a component. Only called by the engine. */
    public final void engineDestroy() {
        if (mGameObject != null) {
            mGameObject.removeComponent(this);
        }

        mReference.clear();
    }

    /** Handle component's removal. Called by game object. */
    public final void onRemove() {
        if (mAwake) {
            onDestroy();
        }
        mGameObject = null;
        mReference.clear();
    }

    /**
     * Set the component's enabled state.
     *
     * @param enabled whether to enable or disable the component.
     */
    public void setEnabled(boolean enabled) {
        if (enabled != mEnabled && mGameObject != null) {
            mGameObject.dirtyComponentLists();
        }
        mEnabled = enabled;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    protected abstract void onDestroy();

    /**
     * Getter for mReference.
     *
     * <p>This getter type checks and casts the reference to a more concrete type
     *
     * @param <T> type to try and cast to.
     * @param type class object of type T.
     * @return mReference cast into the type, if valid. Null otherwise.
     */
    public final <T extends Component> Reference<T> getReference(Class<T> type) {
        return mReference.cast(type);
    }

    /** Clear the current reference of the object and create a new one. */
    public final void recreateReference() {
        mReference.clear();
        mReference = new Reference<>(this);
    }
}
