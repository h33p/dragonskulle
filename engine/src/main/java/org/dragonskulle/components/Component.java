/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
@Accessors(prefix = "m")
public abstract class Component implements Serializable {

    @Getter private final Reference<Component> mReference = new Reference<>(this);

    @Getter @Setter protected GameObject mGameObject;

    @Getter @Setter private boolean mAwake = false;
    @Getter @Setter private boolean mEnabled = true;
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
        onDestroy();

        mGameObject.removeComponent(this);
        mGameObject = null;
        mReference.clear();
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    protected abstract void onDestroy();

    /**
     * Getter for mReference
     *
     * <p>This getter type checks and casts the reference to a more concrete type
     *
     * @param type type to try cast into
     * @return mReference cast into the type, if valid. Null otherwise.
     */
    @SuppressWarnings("unchecked")
    public final <T extends Component> Reference<T> getReference(Class<T> type) {
        if (type.isInstance(this)) return (Reference<T>) mReference;
        return null;
    }
}
