/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.HashMap;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;

/**
 * Stores reference singletons for a given scope.
 *
 * <p>Scope can be anything from {@link Scene}, to custom classes.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class SingletonStore {
    private final HashMap<Class<?>, Reference<Component>> mSingletons = new HashMap<>();

    /**
     * Register a singleton
     *
     * <p>Allows to register a singleton for the scope. Singletons are meant for components that
     * should have only one, or only one main instance of them in the scope.
     *
     * @param comp component to register as a singleton
     * @return {@code true} if registration was successful, {@code false} if there already is a
     *     singleton.
     */
    public boolean register(Component comp) {
        Class<?> type = comp.getClass();
        Reference<Component> current = mSingletons.get(type);

        if (Reference.isValid(current)) {
            return false;
        }

        current = comp.getReference();
        mSingletons.put(type, current);
        return true;
    }

    /**
     * Retrieves a singleton for type if there is any.
     *
     * @param <T> type of the singleton.
     * @param type class of type T to retrieve the instance of.
     * @return reference to the singleton. {@code null} if does not exist, or it has been destroyed.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        Reference<Component> current = mSingletons.get(type);
        if (!Reference.isValid(current)) {
            return null;
        }
        return (T) current.get();
    }

    /**
     * Retrieves a singleton reference for type if there is any.
     *
     * @param <T> type of the singleton.
     * @param type class of type T to retrieve the instance reference of.
     * @return reference to the singleton. For invalid entries, it may be null, but may also be a
     *     non-null invalid reference.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Reference<T> getRef(Class<T> type) {
        Reference<Component> current = mSingletons.get(type);
        return (Reference<T>) current;
    }

    /**
     * Unregisters a singleton.
     *
     * @param type type to unregister
     * @return component reference if there was a singleton
     */
    public Reference<Component> unregister(Class<?> type) {
        return mSingletons.remove(type);
    }
}
